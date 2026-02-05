package com.moneytransfer.domain;

/**
 * Role enum representing user authority levels in the system.
 * 
 * Design principle: Start with least privilege, add power only where required.
 * 
 * Roles:
 * - USER: Default role for account holders (90% of traffic)
 *   Can: Initiate transfers, view own data, access health endpoints
 *   Cannot: Access other users' data, system-wide views, admin functions
 * 
 * - ADMIN: Operational role for support/ops/back-office
 *   Can: View any account, view any transaction, access admin endpoints
 *   Cannot: Initiate money transfers on behalf of users
 *   
 * Critical principle: Roles define authority. Ownership defines access.
 */
public enum Role {
    /**
     * USER - Customer/account holder with self-service capabilities.
     * This is the default role for regular system users.
     */
    USER,
    
    /**
     * ADMIN - Operations/support role with read-only system access.
     * Admins observe the system but do not move money.
     * This prevents insider abuse.
     */
    ADMIN
}
