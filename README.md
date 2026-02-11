# Infernus Bot

Java-based Discord bot built for a real multi-guild service (volunteer client work).  
The service has since been discontinued; all intellectual property reverted to me.  
This repository is preserved as a **portfolio project** for technical review.

Secrets (Discord token, database URL, credentials) have been intentionally **redacted**.

---

## Overview

Infernus is a backend-focused Discord bot designed to support moderation, ticketing, and external API integrations across multiple guilds.  
The project evolved over time as an experimental platform for improving concurrency, database access patterns, and service reliability.

---

## Core Systems

### Database Layer (Custom Pooling)

- Custom MySQL connection pool implemented using a bounded blocking queue
- Pool size: **30 connections**
- Shared fixed thread pool: **25 worker threads**
- Explicit connection lifecycle management:
  - validated borrow
  - validated return
  - replacement of closed or invalid connections

This system was built intentionally to understand pooling internals.  
For production systems, a mature pool such as **HikariCP** would be preferred.

### Moderation & Cross-Guild Synchronization

- Structured sanctions model (temporary bans, permanent bans, kicks, timeouts, warnings)
- Sanctions persist in MySQL
- Enforcement is synchronized across two linked guilds
- Scheduled task automatically expires temporary sanctions and reverses them consistently

### Ticketing System

- Database-backed support ticket system
- Ticket creation via Discord modals
- Automatic private channel provisioning
- Permission-based access for users, staff, and escalation teams
- Ticket state persists independently of Discord uptime

### External API Integration (Roblox)

- Asynchronous HTTP integration using Java `HttpClient`
- CompletableFuture-based execution
- Centralized request logic for:
  - username → user ID resolution
  - group membership verification
  - group rank retrieval

---

## Tooling & AI Assistance

During development, I used AI tools such as ChatGPT as a technical aid to help explore implementation ideas, better understand certain concepts, and refine portions of the code.

The overall architecture, design decisions, and final implementation were developed and tested by me. AI-generated examples were reviewed and adapted where appropriate.

---

## Tech Stack

- Java
- JDA (Discord API)
- MySQL
- Java concurrency utilities (`ExecutorService`, blocking queues)
- Java HTTP Client

---

## Notes

This repository is intended for **code and architecture review** rather than deployment.  
I’m happy to discuss design decisions, tradeoffs, or specific implementations in detail.
