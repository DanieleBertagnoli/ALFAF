from flask import Flask, request, jsonify

app = Flask(__name__)
USER_INFO_FILE = 'known_users.txt'


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
        return handle_emergency(data[1:])

    return jsonify({'status': 'error', 'message': 'Invalid command'}), 400


def register_user(user_info):
    if len(user_info) != 2:
        return jsonify({'status': 'error', 'message': 'Invalid user info'}), 400

    name, phone_number = user_info
    user_info_str = f"{name} {phone_number}\n"
    with open(USER_INFO_FILE, 'a') as file:
        file.write(user_info_str)

    return jsonify({'status': 'success', 'message': 'User registered successfully'}), 200


def handle_emergency(phone_numbers):
    with open('emergency_alert.txt', 'a') as file:
        file.write('Emergency alert received\n')

    return jsonify({'status': 'success', 'message': 'Emergency alert received'}), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
