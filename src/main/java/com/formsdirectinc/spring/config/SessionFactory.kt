package com.formsdirectinc

import com.formsdirectinc.tenant.TenantContextHolder
import org.hibernate.Session
import org.hibernate.SessionFactory

class SessionFactory (val sessionFactoryRegistry: Map<String, SessionFactory>) {

    fun get(): SessionFactory {
        return sessionFactoryRegistry.get(TenantContextHolder.getTenantId())!!
    }

    fun openSession(): Session {
        return get().openSession()
    }

    fun getCurrentSession(): Session {
        return get().currentSession
    }
}