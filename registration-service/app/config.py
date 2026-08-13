from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "eventplatform"
    db_user: str = "postgres"
    db_password: str = "postgres"

    jwt_secret: str = "your-secret-key"

    event_service_url: str = "http://localhost:8082"

    rabbitmq_url: str = "amqp://guest:guest@localhost:5672"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()