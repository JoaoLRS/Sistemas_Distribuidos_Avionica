# Usa uma imagem oficial leve do Python
FROM python:3.11-slim

# Define a pasta de trabalho dentro do contentor
WORKDIR /app

# Permite que os modulos Python publiquem e consumam Kafka
RUN pip install paho-mqtt requests kafka-python

# Copia os módulos Python para dentro do contentor
COPY *.py ./

# O comando padrão (será substituído pelo docker-compose)
CMD ["python"]
