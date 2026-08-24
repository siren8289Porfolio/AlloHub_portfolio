from __future__ import annotations

from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AI_", env_file=".env", extra="ignore")

    # Optional OpenDART key. Without it, evidence returns STUB/UNAVAILABLE style payloads.
    opendart_api_key: Optional[str] = None
    service_name: str = "allochub-ai-service"
    # Deterministic explanation only unless a provider is configured later.
    llm_enabled: bool = False


settings = Settings()
