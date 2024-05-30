import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging
from flask import Flask, request, jsonify

# Initialize Firebase Admin SDK
cred = credentials.Certificate('./fcm.json')
firebase_admin.initialize_app(cred)

app = Flask(__name__)
USER_INFO_FILE = 'known_users.txt'
FCM_TOKENS_FILE = 'fcm_tokens.txt'


@app.route('/', methods=['POST'])
def handle_requests():
    data = request.data.decode('utf-8').split()
    print(data)
    if not data:
        return jsonify({'status': 'error', 'message': 'No message received'}), 400

    command = data[0]
    if command == 'new_user':
        return register_user(data[1:])
    elif command == 'emergency':
        return handle_emergency(data[3:], data[1], data[2])
    elif command == 'register_token':
        return register_fcm_token(data[1:])

    return jsonify({'status': 'error', 'message': 'Invalid command'}), 400


def register_user(user_info):
    if len(user_info) != 2:
        return jsonify({'status': 'error', 'message': 'Invalid user info'}), 400

    name, phone_number = user_info
    phone_number = phone_number.replace('(', '').replace(')', '').replace('-', '')

    user_info_str = f"{name} {phone_number}\n"
    with open(USER_INFO_FILE, 'a') as file:
        file.write(user_info_str)

    return jsonify({'status': 'success', 'message': 'User registered successfully'}), 200


def handle_emergency(phone_numbers, sender, positions):
    if not phone_numbers:
        return jsonify({'status': 'error', 'message': 'No phone numbers provided'}), 400

    with open('emergency_alert.txt', 'a') as file:
        file.write(f'Emergency alert received by {sender}\n')

    for phone_number in phone_numbers:
        phone_number = phone_number.replace('(', '').replace(')', '').replace('-', '')
        user_info = get_user_info(phone_number)
        if user_info:
            name, _ = user_info
            positions = positions.split('_')[:-1]
            send_notification(phone_number, name, sender, positions)
        else:
            print(f'No user info found for {phone_number}')

    return jsonify({'status': 'success', 'message': 'Emergency alerts sent'}), 200


def register_fcm_token(token_info):
    if len(token_info) != 2:
        return jsonify({'status': 'error', 'message': 'Invalid token info'}), 400

    phone_number, fcm_token = token_info
    phone_number = phone_number.replace('(', '').replace(')', '').replace('-', '')

    tokens = {}
    try:
        with open(FCM_TOKENS_FILE, 'r') as file:
            for line in file:
                stored_phone_number, stored_token = line.strip().split()
                tokens[stored_phone_number] = stored_token
    except FileNotFoundError:
        pass

    # Check if the token already exists and update the associated phone number
    for pn, token in tokens.items():
        if token == fcm_token:
            tokens.pop(pn)
            break

    tokens[phone_number] = fcm_token

    with open(FCM_TOKENS_FILE, 'w') as file:
        for pn, token in tokens.items():
            file.write(f"{pn} {token}\n")

    return jsonify({'status': 'success', 'message': 'FCM token registered successfully'}), 200



def send_notification(phone_number, name, sender, positions):
    fcm_token = get_fcm_token(phone_number)
    if fcm_token:
        body = f"{name}, {sender} needs help! Location: "
        for i in range(0, len(positions), 4):
            body += f'\n{positions[i]} {positions[i+1]} {positions[i+2]} {positions[i+3]}'

        message = messaging.Message(
            data={
                'title': "Emergency Alert",
                'body': body
            },
            token=fcm_token
        )

        # Send the message
        response = messaging.send(message)
        print(f'Successfully sent message: {response}')
    else:
        print(f'No FCM token found for {phone_number}')



def get_fcm_token(phone_number):
    try:
        with open(FCM_TOKENS_FILE, 'r') as file:
            for line in file:
                stored_phone_number, fcm_token = line.strip().split()
                if stored_phone_number == phone_number:
                    return fcm_token
    except FileNotFoundError:
        print('FCM tokens file not found.')
    return None


def get_user_info(phone_number):
    try:
        with open(USER_INFO_FILE, 'r') as file:
            for line in file:
                name, stored_phone_number = line.strip().split()
                if stored_phone_number == phone_number:
                    return name, stored_phone_number
    except FileNotFoundError:
        print('User info file not found.')
    return None


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
