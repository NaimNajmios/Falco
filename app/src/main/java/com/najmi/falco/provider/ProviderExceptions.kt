package com.najmi.falco.provider

class AllProvidersFailedException(
    message: String = "All LLM providers failed or are unavailable. Check your API keys or try again later."
) : Exception(message)

class RateLimitException(
    provider: String,
    message: String = "Rate limit exceeded for $provider"
) : Exception(message)
