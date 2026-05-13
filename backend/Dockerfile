FROM python:3.11-slim

# Install build-essential and dos2unix
RUN apt-get update && apt-get install -y \
    build-essential \
    time \
    gawk \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1234 blantgroup &amp;&amp; \
    useradd -m -u 1234 -g blantgroup blantuser

WORKDIR /app
    
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade -r requirements.txt

ENV WORKERS=1
ENV THREADS=4
ENV PATH="$PATH:/app/libwayne/bin"

# COPY everything not in the dockerignore
COPY BLANT .
COPY native_src ./native_src
COPY server.py .

RUN ./native_src/scripts/make_mock.sh

RUN chown -R blantuser:blantgroup /app

USER blantuser

EXPOSE 5000

# production
CMD ["sh", "-c", "exec gunicorn --workers=$WORKERS --threads=$THREADS --access-logfile - server:app -b 0.0.0.0:5000"]

# development
# CMD ["python", "server.py"]