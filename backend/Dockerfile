FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    time \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV WORKERS=1
ENV THREADS=4
ENV PATH="$PATH:/app/libwayne/bin"

ENV PATH="$PATH:/app/libwayne/bin"
# COPY everything not in the dockerignore

# comment out below line if want to test
COPY BLANT .

COPY native_src ./native_src
COPY requirements.txt .
COPY server.py .

RUN pip install --no-cache-dir --upgrade -r requirements.txt
RUN ./native_src/scripts/make_mock.sh

EXPOSE 5000

# Fix Windows line endings --- can comment out below when ready to deploy
RUN dos2unix native_src/scripts/run_mock.sh 
RUN chmod +x native_src/scripts/run_mock.sh

RUN dos2unix native_src/scripts/make_mock.sh 
RUN chmod +x native_src/scripts/make_mock.sh

RUN ./native_src/scripts/make_mock.sh
RUN chmod +x native_src/mock/_objs/mock_blant
# --- 

# comment out if want to test
RUN ./regression-test-all.sh -make

# production
# CMD gunicorn --workers $WORKERS --threads $THREADS server:app -b 0.0.0.0:5000
CMD ["sh", "-c", "exec gunicorn --workers=$WORKERS --threads=$THREADS server:app -b 0.0.0.0:5000"]

# development
# CMD ["python", "server.py"]