#!/usr/bin/env python3
"""Owner-run helper. Requires authenticated GitHub CLI; never prints secret values."""
import argparse
import base64
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('keystore', type=Path)
parser.add_argument('recovery_file', type=Path)
parser.add_argument('--repo', default='DV8FromTheWorld/ChromeBridge')
args = parser.parse_args()
fields = dict(line.split(': ', 1) for line in args.recovery_file.read_text().splitlines() if ': ' in line)
secrets = {
    'CHROMEBRIDGE_KEYSTORE_BASE64': base64.b64encode(args.keystore.read_bytes()).decode('ascii'),
    'CHROMEBRIDGE_KEYSTORE_PASSWORD': fields['Keystore password'],
    'CHROMEBRIDGE_KEY_ALIAS': fields['Key alias'],
    'CHROMEBRIDGE_KEY_PASSWORD': fields['Key password'],
}
if not all(secrets.values()):
    raise SystemExit('Missing a required value; no changes made.')
for name, value in secrets.items():
    # Pass only on stdin, not command-line arguments, shell interpolation or logs.
    result = subprocess.run(['gh', 'secret', 'set', name, '--repo', args.repo], input=value.encode(), capture_output=True)
    if result.returncode:
        raise SystemExit(f'Could not save {name}. Earlier successful secrets may already be saved; inspect GitHub before retrying.')
    print(f'Saved {name}')
print('Secret setup complete. Retain your keystore and passwords in your password manager.')
