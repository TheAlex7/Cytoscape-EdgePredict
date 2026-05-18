FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    time \
    gawk \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1234 blantgroup && \
    useradd -m -u 1234 -g blantgroup blantuser

WORKDIR /app

ENV WORKERS=2
ENV THREADS=4
ENV PATH="$PATH:/app/libwayne/bin"

# COPY everything not in the dockerignore
COPY --chown=blantuser:blantgroup BLANT .
COPY --chown=blantuser:blantgroup native_src ./native_src
COPY --chown=blantuser:blantgroup server.py .
COPY --chown=blantuser:blantgroup requirements.txt .

RUN pip install --no-cache-dir --upgrade -r requirements.txt
RUN ./native_src/scripts/make_mock.sh

RUN chown -R blantuser:blantgroup /app

USER blantuser

EXPOSE 5000

# production
# CMD ["sh", "-c", "exec gunicorn --workers=$WORKERS --threads=$THREADS --access-logfile - server:app -b 0.0.0.0:5000"]

# If a keys dir was mounted as a  volume then it will use the key files as such for https 
CMD ["/bin/sh", "-c", "if [ -d '/keys' ]; then \
    gunicorn --workers=$WORKERS --threads=$THREADS --access-logfile - server:app -b 0.0.0.0:5000 --keyfile /keys/key.pem --certfile /keys/crt.pem; \
    else \
    gunicorn --workers=$WORKERS --threads=$THREADS --access-logfile - server:app -b 0.0.0.0:5000; fi"]

# development
# CMD ["python", "server.py"]