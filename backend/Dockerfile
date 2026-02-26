FROM python:3.11-slim

# Install build-essential
RUN apt-get update && apt-get install -y \
    build-essential 

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY mock ./mock

COPY Makefile .
RUN make

COPY run_mock.sh .
COPY say_hello.sh .

COPY app.py .

EXPOSE 5000

CMD ["python", "app.py"]