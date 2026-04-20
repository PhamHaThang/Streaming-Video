# ═══════════════════════════════════════
# Recommendation Service - Python
# ═══════════════════════════════════════
FROM python:3.11-slim

# Metadata
LABEL service="recommendation-service"
LABEL maintainer="streaming-video-team"

WORKDIR /app

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY . .

EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

# Run with uvicorn (production mode, no reload)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8085"]
