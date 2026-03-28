package com.najmi.falco.di

import com.najmi.falco.data.remote.LlmProvider
import dagger.MapKey

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LlmProviderKey(val value: LlmProvider)
