#!/usr/bin/env python3
"""Compile and run production notification text rules independently of Gradle/Android."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, artifact, version):
    values = sorted((CACHE / group / artifact / version).glob('*/*.jar'))
    if not values:
        raise SystemExit(f'Missing test dependency: {group}:{artifact}:{version}')
    return str(values[0])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.4.10')
compiler = [jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.4.10'), stdlib,
            jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.3.21'),
            jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.4.10'),
            jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.10.2'),
            jar('org.jetbrains', 'annotations', '13.0')]
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
with tempfile.TemporaryDirectory(prefix='hchat-notification-tests-') as work:
    output = str(Path(work) / 'tests.jar')
    subprocess.run([java, '-Xmx256m', '-cp', os.pathsep.join(compiler),
                    'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
                    '-classpath', stdlib, '-d', output,
                    'app/src/main/java/h/Hchat/hooks/items/customnotify/CustomNotificationText.kt',
                    'scripts/tests/custom_notification/CustomNotificationTextRegression.kt'], cwd=ROOT, check=True)
    subprocess.run([java, '-cp', os.pathsep.join([output, stdlib]),
                    'h.Hchat.hooks.items.customnotify.CustomNotificationTextRegressionKt'], cwd=ROOT, check=True)
