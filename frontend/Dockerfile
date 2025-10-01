FROM python:3.11-slim

# Системні пакети (tini для акуратного завершення)
RUN apt-get update && apt-get install -y --no-install-recommends bash ca-certificates curl tini \
 && rm -rf /var/lib/apt/lists/*

# Робоча директорія і користувач
WORKDIR /home/appuser
RUN groupadd -g 1000 appuser && useradd -u 1000 -g 1000 -ms /bin/bash appuser

# Кладемо requirements ПЕРЕД кодом (для кешу). Контекст = папка frontend/
COPY requirements.txt /home/appuser/requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
 && pip install --no-cache-dir -r /home/appuser/requirements.txt

# Кладемо увесь код (з контексту frontend/)
COPY . /home/appuser/

# Критично: прибрати CRLF і дати права виконання саме В ОБРАЗІ
RUN sed -i 's/\r$//' /home/appuser/run.sh && chmod 755 /home/appuser/run.sh

USER appuser
EXPOSE 8501

ENTRYPOINT ["/usr/bin/tini","--"]
CMD ["/home/appuser/run.sh"]
