FROM python:3.11-slim

# Install build-essential
RUN apt-get update && apt-get install -y \
    build-essential 

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

# Copy and compile
COPY hello.c .
RUN gcc hello.c -o hello 

RUN chmod +x run_mock.sh

COPY app.py .

EXPOSE 5000

CMD ["python", "app.py"]