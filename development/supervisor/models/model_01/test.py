import json
import os

# Чтение данных из входного файла
with open(os.environ.get('MODEL_INPUT', '/input/start.json'), 'r') as file:
    data = json.load(file)

# Извлечение значений из данных
E_input = data["E_input"]
h_y_1 = data["h_y_1"]
h_y_2 = data["h_y_2"]
h_x_1 = data["h_x_1"]
h_x_2 = data["h_x_2"]

# Вычисление E_output
E_output = E_input * (1 - (h_y_1 + h_y_2 + h_x_1 + h_x_2) / 3.3)

# Запись результата в выходной файл
output_data = {
    'E_start': E_output,
    'E_end': E_output
}

with open(os.environ.get('MODEL_OUTPUT', '/output/end.json'), 'w') as file:
    json.dump(output_data, file, indent=2)

'''
print(f"E_output = {E_output}")
print("Результат записан в файл output_data.json")
'''