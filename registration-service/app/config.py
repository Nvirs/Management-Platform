from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# runs fall back to plain env vars / .env.
_SECRETS_DIR = "/run/secrets"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        secrets_dir=_SECRETS_DIR if Path(_SECRETS_DIR).is_dir() else None,
    )

    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "eventplatform"
    db_user: str = "postgres"
    db_password: str = "postgres"

    jwt_secret: str = "your-secret-key"

    event_service_url: str = "http://localhost:8082"

    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "guest"
    rabbitmq_password: str = "guest"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    @property
    def rabbitmq_url(self) -> str:
        return (
            f"amqp://{self.rabbitmq_user}:{self.rabbitmq_password}"
            f"@{self.rabbitmq_host}:{self.rabbitmq_port}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()