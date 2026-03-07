#!/usr/bin/env python3
"""
Telegram Bot API file uploader
Uploads files to Telegram channel via Bot API using multipart/form-data.
"""
import urllib.request
import urllib.error
import sys
import os

def pin_message(bot_token, chat_id, message_id):
    """Pin a message in Telegram channel."""
    import urllib.request
    import urllib.parse
    import json

    url = f"https://api.telegram.org/bot{bot_token}/pinChatMessage"
    data = urllib.parse.urlencode({
        'chat_id': chat_id,
        'message_id': message_id,
        'disable_notification': True
    }).encode()

    try:
        req = urllib.request.Request(url, data=data)
        response = urllib.request.urlopen(req, timeout=10)
        result = json.loads(response.read().decode())
        if result.get('ok'):
            print(f"📌 Pinned message {message_id}")
            return True
        else:
            print(f"⚠️  Failed to pin: {result.get('description', 'Unknown error')}")
            return False
    except Exception as e:
        print(f"⚠️  Failed to pin message: {e}")
        return False

def upload_to_telegram(bot_token, chat_id, file_path, caption=""):
    """Upload file to Telegram via Bot API."""

    if not os.path.exists(file_path):
        print(f"ERROR: File not found: {file_path}", file=sys.stderr)
        return False

    url = f"https://api.telegram.org/bot{bot_token}/sendDocument"

    # Read file
    file_size = os.path.getsize(file_path)
    print(f"Uploading: {file_path} ({file_size:,} bytes)")

    with open(file_path, 'rb') as f:
        file_data = f.read()

    filename = os.path.basename(file_path)

    # Determine MIME type
    if filename.endswith('.apk'):
        mime_type = 'application/vnd.android.package-archive'
    elif filename.endswith('.txt'):
        mime_type = 'text/plain'
    else:
        mime_type = 'application/octet-stream'

    # Create multipart/form-data
    boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
    body_parts = []

    # chat_id field
    body_parts.append(f'--{boundary}\r\n'.encode())
    body_parts.append(f'Content-Disposition: form-data; name="chat_id"\r\n\r\n'.encode())
    body_parts.append(f'{chat_id}\r\n'.encode())

    # caption field (if provided)
    if caption:
        body_parts.append(f'--{boundary}\r\n'.encode())
        body_parts.append(f'Content-Disposition: form-data; name="caption"\r\n\r\n'.encode())
        body_parts.append(f'{caption}\r\n'.encode())

    # document field (file)
    body_parts.append(f'--{boundary}\r\n'.encode())
    body_parts.append(f'Content-Disposition: form-data; name="document"; filename="{filename}"\r\n'.encode())
    body_parts.append(f'Content-Type: {mime_type}\r\n\r\n'.encode())
    body_parts.append(file_data)
    body_parts.append(f'\r\n--{boundary}--\r\n'.encode())

    body = b''.join(body_parts)

    headers = {
        'Content-Type': f'multipart/form-data; boundary={boundary}'
    }

    print(f"Uploading to Telegram (timeout: 5 minutes)...")
    req = urllib.request.Request(url, data=body, headers=headers)

    try:
        response = urllib.request.urlopen(req, timeout=300)
        print(f"✅ SUCCESS: HTTP {response.status}")

        # Parse and print result
        import json
        result = json.loads(response.read().decode())
        if result.get('ok'):
            message_id = result['result'].get('message_id')
            chat_title = result['result']['chat'].get('title', 'Unknown')
            print(f"Message ID: {message_id}")
            print(f"Chat: {chat_title}")

            # Pin the message if it's an APK file
            if file_path.endswith('.apk'):
                pin_message(bot_token, chat_id, message_id)

        return True

    except urllib.error.HTTPError as e:
        print(f"❌ FAILED: HTTP {e.code}", file=sys.stderr)
        print(e.read().decode(), file=sys.stderr)
        return False

    except Exception as e:
        print(f"❌ ERROR: {e}", file=sys.stderr)
        return False

def main():
    if len(sys.argv) < 4:
        print("Usage: python3 telegram_upload.py <bot_token> <chat_id> <file_path> [caption]")
        sys.exit(1)

    bot_token = sys.argv[1]
    chat_id = sys.argv[2]
    file_path = sys.argv[3]
    caption = sys.argv[4] if len(sys.argv) > 4 else ""

    # Hide token in output
    print("━" * 80)
    print("WinoPay - Telegram Upload")
    print("━" * 80)
    print(f"Bot: {bot_token[:10]}...(hidden)")
    print(f"Chat: {chat_id}")
    print(f"File: {file_path}")
    if caption:
        print(f"\nCaption:\n{caption}")
    print("━" * 80)

    success = upload_to_telegram(bot_token, chat_id, file_path, caption)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
