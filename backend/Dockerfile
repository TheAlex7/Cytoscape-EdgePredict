FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    dos2unix

WORKDIR /app

# COPY everything not in the dockerignore
COPY . .

RUN pip install --no-cache-dir --upgrade -r requirements.txt

RUN make

# Fix Windows line endings
RUN dos2unix src/scripts/run_mock.sh

RUN chmod +x src/scripts/run_mock.sh
RUN chmod +x src/mock/_objs/mock_blant

EXPOSE 5000

# production
CMD ["gunicorn", "server:app", "-b", "0.0.0.0:5000"]