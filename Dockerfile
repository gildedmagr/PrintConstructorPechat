# Stage 1: Build the Java application using a Maven image
FROM maven:3.9.4-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files (pom.xml and source code)
# This helps leverage Docker's layer caching for faster builds if only source changes
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests for faster image creation
# The 'package' goal will create a JAR file in the 'target' directory
RUN mvn clean compile assembly:single -DskipTests

# Stage 2: Create a smaller runtime image with only the JRE
FROM eclipse-temurin:21-alpine

# Set the working directory for the final application
WORKDIR /app

# Copy the built JAR file from the 'build' stage into the final image
# Assumes your Maven build produces a JAR named 'your-java-app.jar' in 'target/'
COPY examples ./examples
COPY render.properties ./render.properties
COPY --from=build /app/build/render.jar .
COPY wait-for-it.sh /usr/local/bin/wait-for-it
RUN chmod +x /usr/local/bin/wait-for-it

#COPY --from=build /app/target/*.jar ./your-java-app.jar


# Define the command to run your Java application
# The SELENIUM_HOST and SELENIUM_PORT environment variables
# will be automatically passed from docker-compose.yml to this container.
#CMD ["java", "-jar", "build/render.jar"]

# Add comments to explain each step
# FROM: Specifies the base image for the current stage.
# WORKDIR: Sets the working directory for any RUN, CMD, ENTRYPOINT, COPY, or ADD instructions that follow.
# COPY: Copies new files or directories from <src> and adds them to the filesystem of the container at the path <dest>.
# RUN: Executes any commands in a new layer on top of the current image and commits the results.
# CMD: Provides defaults for an executing container. There can only be one CMD instruction in a Dockerfile.
#      If you specify a CMD, it will be executed when the container starts.
