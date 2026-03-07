#!/usr/bin/env python3
"""
Add secrets to GitHub repository using GitHub API.
Requires: GITHUB_TOKEN environment variable with repo scope.
"""
import urllib.request
import urllib.error
import json
import os
import sys
import base64
from nacl import encoding, public

def get_public_key(token, repo):
    """Get repository public key for encrypting secrets."""
    url = f"https://api.github.com/repos/{repo}/actions/secrets/public-key"
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"token {token}")
    req.add_header("Accept", "application/vnd.github.v3+json")

    try:
        response = urllib.request.urlopen(req)
        return json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        print(f"❌ Error getting public key: {e.code}")
        print(e.read().decode())
        sys.exit(1)

def encrypt_secret(public_key, secret_value):
    """Encrypt a secret using the repository's public key."""
    public_key_bytes = base64.b64decode(public_key)
    sealed_box = public.SealedBox(public.PublicKey(public_key_bytes))
    encrypted = sealed_box.encrypt(secret_value.encode("utf-8"))
    return base64.b64encode(encrypted).decode("utf-8")

def add_secret(token, repo, secret_name, secret_value, key_id, public_key):
    """Add or update a secret in the repository."""
    encrypted_value = encrypt_secret(public_key, secret_value)

    url = f"https://api.github.com/repos/{repo}/actions/secrets/{secret_name}"
    data = json.dumps({
        "encrypted_value": encrypted_value,
        "key_id": key_id
    }).encode()

    req = urllib.request.Request(url, data=data, method='PUT')
    req.add_header("Authorization", f"token {token}")
    req.add_header("Accept", "application/vnd.github.v3+json")
    req.add_header("Content-Type", "application/json")

    try:
        response = urllib.request.urlopen(req)
        return response.status in [201, 204]
    except urllib.error.HTTPError as e:
        print(f"❌ Error adding secret {secret_name}: {e.code}")
        print(e.read().decode())
        return False

def main():
    # Check for PyNaCl
    try:
        from nacl import encoding, public
    except ImportError:
        print("❌ ERROR: PyNaCl not installed")
        print("\nInstall it with:")
        print("  pip3 install PyNaCl")
        print("\nOr use the shell script instead:")
        print("  ./scripts/setup_github_secrets.sh")
        sys.exit(1)

    # Get GitHub token
    github_token = os.environ.get('GITHUB_TOKEN')
    if not github_token:
        print("❌ ERROR: GITHUB_TOKEN environment variable not set")
        print("\nCreate a token at: https://github.com/settings/tokens/new")
        print("Required scope: repo")
        print("\nThen run:")
        print("  export GITHUB_TOKEN='your_token_here'")
        print("  python3 scripts/add_github_secrets.py OWNER/REPO")
        sys.exit(1)

    # Get repository
    if len(sys.argv) < 2:
        print("Usage: python3 add_github_secrets.py OWNER/REPO")
        print("\nExample:")
        print("  python3 add_github_secrets.py yourusername/WinoPay")
        sys.exit(1)

    repo = sys.argv[1]

    # Load secrets from .env
    env_file = '.env'
    if not os.path.exists(env_file):
        print(f"❌ ERROR: {env_file} not found")
        sys.exit(1)

    print(f"Loading secrets from {env_file}...")
    secrets = {}
    with open(env_file, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                secrets[key.strip()] = value.strip()

    # Required secrets
    required = ['TG_BOT_TOKEN', 'TG_CHAT_ID']
    for key in required:
        if key not in secrets or not secrets[key]:
            print(f"❌ ERROR: {key} not found in {env_file}")
            sys.exit(1)

    print(f"\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print(f"Adding secrets to {repo}")
    print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    # Get public key
    print(f"\nGetting repository public key...")
    key_data = get_public_key(github_token, repo)
    key_id = key_data['key_id']
    public_key = key_data['key']
    print(f"✅ Public key: {key_id}")

    # Add secrets
    secrets_to_add = {
        'TG_BOT_TOKEN': secrets['TG_BOT_TOKEN'],
        'TG_CHAT_ID': secrets['TG_CHAT_ID']
    }

    if 'REOWN_PROJECT_ID' in secrets and secrets['REOWN_PROJECT_ID']:
        secrets_to_add['REOWN_PROJECT_ID'] = secrets['REOWN_PROJECT_ID']

    print(f"\nAdding {len(secrets_to_add)} secrets...")
    for name, value in secrets_to_add.items():
        print(f"  {name}... ", end='', flush=True)
        if add_secret(github_token, repo, name, value, key_id, public_key):
            print("✅")
        else:
            print("❌")

    print(f"\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print(f"✅ Secrets configured!")
    print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print(f"\nView at: https://github.com/{repo}/settings/secrets/actions")

if __name__ == '__main__':
    main()
