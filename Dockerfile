# Use a imagem base do OpenJDK 17
FROM openjdk:17-jdk-slim

# Defina o diretório de trabalho dentro do container
WORKDIR /app

# Crie o diretório /videos e suas subpastas
RUN mkdir -p /videos/raw-videos /videos/processed-videos && chmod -R 777 /videos

# Copie o arquivo JAR da aplicação para o diretório /app
COPY target/video-editor-api-0.0.1-SNAPSHOT.jar /app/video-editor-api.jar

# Copie o arquivo .env
COPY .env .env

# Atualize o sistema e instale o ffmpeg
RUN apt-get update && apt-get install -y ffmpeg

# Crie o diretório .aws
RUN mkdir -p /root/.aws

# Copie os arquivos de configuração do AWS
COPY .docker_aws/credentials /root/.aws/credentials
COPY .docker_aws/config /root/.aws/config

# Exponha a porta 8080
EXPOSE 8080

# Comando para executar a aplicação quando o container iniciar
CMD ["java", "-jar", "/app/video-editor-api.jar"]

