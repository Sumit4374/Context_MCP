.PHONY: dev test docker-up docker-down migrate clean build verify

# Start PostgreSQL via Docker Compose
docker-up:
	docker compose up -d
	@echo "Waiting for PostgreSQL to be ready..."
	@docker compose exec postgres pg_isready -U context_user -d context_mcp || sleep 3

# Stop Docker Compose
docker-down:
	docker compose down

# Run the application in dev mode
dev: docker-up
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run unit tests (excludes integration tests)
test:
	./mvnw test

# Run all tests including integration (requires Docker for Testcontainers)
verify:
	./mvnw verify

# Build the project
build:
	./mvnw clean package -DskipTests

# Apply Flyway migrations manually
migrate: docker-up
	./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/context_mcp \
		-Dflyway.user=context_user -Dflyway.password=context_dev_password

# Clean build artifacts
clean:
	./mvnw clean
	rm -rf target/
