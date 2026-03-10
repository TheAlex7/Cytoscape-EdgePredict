FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    dos2unix

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade -r requirements.txt

COPY mock ./mock
COPY src ./src 
COPY Makefile .
RUN make

COPY scripts/run_mock.sh .
COPY scripts/say_hello.sh .

# Fix Windows line endings
RUN dos2unix run_mock.sh
RUN dos2unix say_hello.sh
RUN chmod +x run_mock.sh
RUN chmod +x say_hello.sh
RUN chmod +x mock/_objs/mock_blant

COPY app.py .

EXPOSE 5000

# production
CMD ["gunicorn", "server:app", "-b", "0.0.0.0:5000"]

# CMD ["python", "app.py"]