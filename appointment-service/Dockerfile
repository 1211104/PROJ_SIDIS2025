# 1. Usar uma imagem base com Java 17 (leve, versão Alpine)
FROM eclipse-temurin:17-jdk-alpine

# 2. Definir diretório de trabalho dentro do contentor
WORKDIR /app

# 3. Copiar o ficheiro .jar gerado pelo Maven para dentro do contentor
# (O asterisco * garante que apanha o nome independentemente da versão)
COPY target/*.jar app.jar

# 4. Comando para arrancar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]