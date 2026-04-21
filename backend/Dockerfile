FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    dos2unix \
    time \
    gawk \
    git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV PATH="$PATH:/app/libwayne/bin"
# COPY everything not in the dockerignore
COPY BLANT .

COPY native_src ./native_src
COPY requirements.txt .
COPY server.py .

RUN pip install --no-cache-dir --upgrade -r requirements.txt

EXPOSE 5000

# Fix Windows line endings --- can comment out below and add to .dockerignore when ready to deploy
RUN dos2unix native_src/scripts/run_mock.sh 
RUN chmod +x native_src/scripts/run_mock.sh

RUN dos2unix native_src/scripts/make_mock.sh 
RUN chmod +x native_src/scripts/make_mock.sh
RUN ./native_src/scripts/make_mock.sh
RUN chmod +x native_src/mock/_objs/mock_streams

# RUN chmod +x native_src/mock/_objs/mock_blant
# --- 

# uncomment below line if want to use BLANT
# RUN ./regression-test-all.sh -make
# RUN chmod +x ./blant


# production
# CMD ["gunicorn", "server:app", "-b", "0.0.0.0:5000"]

# development
CMD ["python", "server.py"]