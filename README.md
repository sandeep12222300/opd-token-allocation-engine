# OPD Token Allocation Engine

A sophisticated token allocation system for Outpatient Department (OPD) management that intelligently distributes patient appointments across multiple doctors and time slots using a priority-based algorithm with dynamic reallocation capabilities.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [API Design](#api-design)
- [Data Schema](#data-schema)
- [Token Allocation Algorithm](#token-allocation-algorithm)
- [Prioritization Logic](#prioritization-logic)
- [Edge Cases](#edge-cases)
- [Failure Handling](#failure-handling)
- [Getting Started](#getting-started)
- [OPD Day Simulation](#opd-day-simulation)
- [Examples](#examples)

## Overview

The OPD Token Allocation Engine is a Spring Boot application designed to manage patient token allocations efficiently in a hospital's outpatient department. It supports multiple token sources (Emergency, Paid, Follow-up, Online, Walk-in) and implements a fair, priority-based allocation system with dynamic reallocation capabilities.

### Key Capabilities
- **Multi-source token management**: Handles emergency, paid, follow-up, online, and walk-in patients
- **Dynamic priority calculation**: Considers base priority, waiting time, and reallocation history
- **Smart reallocation**: Automatically moves lower-priority tokens to waiting queue when higher-priority patients arrive
- **Doctor efficiency management**: Adjusts slot capacity based on doctor performance
- **Cancellation handling**: Supports token cancellation with automatic waitlist promotion

## Features

1. **Priority-Based Allocation**: Tokens are allocated based on calculated priority scores
2. **Dynamic Aging**: Waiting time increases effective priority to prevent starvation
3. **Fairness Mechanism**: Reallocation penalties prevent infinite preemption
4. **Multi-Doctor Support**: Manages multiple doctors with different efficiency scores
5. **Time Slot Management**: Supports multiple time slots per doctor with capacity management
6. **Waiting Queue**: Automatically manages overflow tokens in priority-ordered queues
7. **Real-time Simulation**: Includes a simulation service for testing scenarios

## API Design

### 1. Token Creation Endpoint

**POST** `/tokens`

Allocates a new token for a patient to a specific doctor and time slot.

**Request Body:**
```json
{
  "doctorId": "D1",
  "slotId": "9-10",
  "patientId": "P001",
  "source": "EMERGENCY"
}
```

**Response:**
```json
{
  "tokenId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ALLOCATED",
  "reason": "Token allocated successfully within slot capacity"
}
```

**Status Values:**
- `ALLOCATED`: Token successfully allocated to the slot
- `WAITLISTED`: Slot is full; token added to waiting queue
- `REALLOCATED_LOW_PRIORITY`: A lower-priority token was moved to waiting queue
- `ERROR`: Request failed (invalid doctor or slot)

### 2. Token Cancellation Endpoint

**POST** `/tokens/cancel`

Cancels an existing token and promotes a waiting patient if available.

**Query Parameters:**
- `doctorId` (required): Doctor identifier
- `slotId` (required): Time slot identifier
- `tokenId` (required): Token identifier to cancel

**Response:**
```
Token cancelled successfully by patient
```
or
```
Token not found or already cancelled
```

## Data Schema

### Core Models

#### Token
Represents a patient appointment token.

```java
{
  "tokenId": "UUID",           // Unique identifier
  "patientId": "String",       // Patient identifier
  "source": "TokenSource",     // Token origin (EMERGENCY, PAID, FOLLOW_UP, ONLINE, WALK_IN)
  "basePriority": "int",       // Initial priority based on source
  "reallocationCount": "int",  // Number of times preempted
  "createdAt": "LocalDateTime", // Creation timestamp
  "allocated": "boolean"       // Current allocation status
}
```

#### Doctor
Represents a doctor with efficiency scoring.

```java
{
  "doctorId": "String",              // Unique identifier
  "efficiencyScore": "double",       // Performance multiplier (affects capacity)
  "slots": "Map<String, TimeSlot>"   // Available time slots
}
```

#### TimeSlot
Represents a time period for appointments.

```java
{
  "slotId": "String",                      // Time slot identifier (e.g., "9-10")
  "baseCapacity": "int",                   // Base patient capacity
  "capacity": "int",                       // Effective capacity (adjusted by efficiency)
  "allocatedTokens": "PriorityQueue<Token>", // Currently allocated tokens
  "waitingQueue": "PriorityQueue<Token>"     // Overflow waiting tokens
}
```

#### TokenSource (Enum)
Defines the origin of a token request.

```java
enum TokenSource {
  EMERGENCY,   // Base Priority: 100
  PAID,        // Base Priority: 85
  FOLLOW_UP,   // Base Priority: 65
  ONLINE,      // Base Priority: 50
  WALK_IN      // Base Priority: 40
}
```

### DTOs (Data Transfer Objects)

#### TokenRequestDTO
```java
{
  "doctorId": "String",
  "slotId": "String",
  "patientId": "String",
  "source": "TokenSource"
}
```

#### AllocationResponseDTO
```java
{
  "tokenId": "String",
  "status": "String",
  "reason": "String"
}
```

## Token Allocation Algorithm

The allocation engine implements a sophisticated priority-based allocation system with the following workflow:

### Algorithm Steps

1. **Priority Calculation**
   ```
   Effective Priority = Base Priority + (Waiting Time × Aging Factor) - (Reallocation Count × Penalty)
   ```
   - **Base Priority**: Determined by token source (Emergency: 100, Paid: 85, Follow-up: 65, Online: 50, Walk-in: 40)
   - **Aging Factor**: 0.3 points per minute of waiting
   - **Reallocation Penalty**: 10 points per preemption

2. **Capacity Check**
   - If slot has available capacity → **ALLOCATE** token directly
   - If slot is full → Proceed to priority comparison

3. **Priority Comparison** (when slot is full)
   - Calculate effective priority of new token
   - Find lowest-priority token in allocated queue
   - If new token priority > lowest token priority:
     - Move lowest-priority token to waiting queue
     - Increment its reallocation counter
     - Allocate new token
     - Return **REALLOCATED_LOW_PRIORITY**
   - Otherwise:
     - Add new token to waiting queue
     - Return **WAITLISTED**

4. **Cancellation Handling**
   - Remove cancelled token from allocated queue
   - If waiting queue is not empty:
     - Promote highest-priority waiting token
     - Move it to allocated queue

### Algorithm Characteristics

- **Time Complexity**: O(log n) for insertion/removal operations
- **Space Complexity**: O(n) where n is total number of tokens
- **Fairness**: Prevents infinite preemption through reallocation penalties
- **Starvation Prevention**: Aging mechanism ensures long-waiting tokens get priority

## Prioritization Logic

### Base Priority Assignment

Token priorities are assigned based on their source:

| Source     | Base Priority | Rationale                                    |
|------------|---------------|----------------------------------------------|
| EMERGENCY  | 100           | Critical medical situations require immediate attention |
| PAID       | 85            | Premium service with guaranteed appointment  |
| FOLLOW_UP  | 65            | Continuing care for existing conditions      |
| ONLINE     | 50            | Pre-booked appointments through portal       |
| WALK_IN    | 40            | General walk-in patients                     |

### Dynamic Priority Adjustment

#### 1. Aging Mechanism
```java
Aging Boost = Waiting Time (minutes) × 0.3
```
**Purpose**: Prevents starvation of lower-priority tokens

**Example**: 
- A walk-in patient (priority 40) waiting 100 minutes gains 30 points → effective priority = 70
- This allows them to potentially overtake newer online bookings (priority 50)

#### 2. Reallocation Penalty
```java
Priority Reduction = Reallocation Count × 10
```
**Purpose**: Prevents infinite preemption cycles

**Example**:
- An emergency token (priority 100) that has been reallocated 3 times:
  - Base: 100
  - Penalty: 3 × 10 = 30
  - Effective Priority: 70 (before aging)

### Priority Queue Management

Both allocated and waiting queues use **max-heap** data structures (PriorityQueue with custom comparator) ensuring:
- O(1) access to highest/lowest priority token
- O(log n) insertion and removal
- Automatic re-ordering as priorities change dynamically

## Edge Cases

The system handles various edge cases robustly:

### 1. Invalid Doctor or Slot
**Scenario**: Request for non-existent doctor or time slot

**Handling**:
```java
if (doctor == null) {
    return new AllocationResponseDTO(null, "ERROR", "Doctor not found");
}
if (slot == null) {
    return new AllocationResponseDTO(null, "ERROR", "Slot not found");
}
```

**Result**: Returns error response without crashing

### 2. Empty Waiting Queue on Cancellation
**Scenario**: Token cancelled but no patients waiting

**Handling**:
```java
if (removed && !slot.getWaitingQueue().isEmpty()) {
    slot.getAllocatedTokens().add(slot.getWaitingQueue().poll());
}
```

**Result**: Simply removes token; no waiting patient promotion

### 3. Simultaneous Equal Priority Tokens
**Scenario**: Multiple tokens with identical calculated priority

**Handling**: Priority queue uses token creation timestamp as secondary comparator through natural ordering, ensuring FIFO behavior for equal priorities

### 4. Doctor Efficiency Changes
**Scenario**: Doctor experiences delays, reducing efficiency

**Handling**:
```java
public void applyDelay(double delayFactor) {
    this.efficiencyScore *= delayFactor;
    for (TimeSlot slot : slots.values()) {
        int newCapacity = (int) (slot.getBaseCapacity() * efficiencyScore);
        slot.updateCapacity(newCapacity);
    }
}
```

**Result**: Dynamically adjusts all slot capacities

### 5. Negative Effective Priority
**Scenario**: Token with many reallocations may calculate negative priority

**Handling**: System allows negative priorities; such tokens naturally sink to bottom of queue, eventually benefiting from aging mechanism

### 6. Capacity Reduction During Operation
**Scenario**: Slot capacity reduced while tokens allocated

**Handling**: 
```java
slot.updateCapacity(Math.max(0, newCapacity));
```
Ensures capacity never goes negative; existing allocations remain valid but new allocations restricted

### 7. Token Cancellation for Non-Existent Token
**Scenario**: Attempt to cancel token that doesn't exist

**Handling**: 
```java
boolean removed = slot.getAllocatedTokens()
    .removeIf(token -> token.getTokenId().equals(tokenId));
return removed;
```

**Result**: Returns false, indicating cancellation failed

## Failure Handling

### Input Validation Failures

**Problem**: Invalid or missing request parameters

**Response**:
```json
{
  "tokenId": null,
  "status": "ERROR",
  "reason": "Doctor not found"
}
```

**HTTP Status**: 200 OK (error indicated in response body)

### Null Pointer Prevention

**Strategy**: Defensive null checks before accessing doctor or slot data

```java
var doctor = InMemoryStore.doctors.get(doctorId);
if (doctor == null) {
    return new AllocationResponseDTO(null, "ERROR", "Doctor not found");
}

var slot = doctor.getSlots().get(slotId);
if (slot == null) {
    return new AllocationResponseDTO(null, "ERROR", "Slot not found");
}
```

### Concurrent Access Handling

**Current State**: In-memory implementation assumes single-threaded access

**Production Consideration**: For multi-threaded environments, implement:
- Synchronization on slot operations
- Thread-safe data structures (ConcurrentHashMap)
- Optimistic locking for database-backed implementations

### System Resilience

**Logging**: Console logging provides audit trail
```java
System.out.println("ALLOCATED → Token added");
System.out.println("Allocated tokens: " + slot.getAllocatedTokens().size());
System.out.println("Waiting queue: " + slot.getWaitingQueue().size());
```

**State Consistency**: All operations are atomic at the slot level, ensuring consistent state even if errors occur

### Recovery Mechanisms

1. **Automatic Waitlist Promotion**: On cancellation, system automatically fills slots from waiting queue
2. **No Token Loss**: Tokens either in allocated queue or waiting queue; never lost
3. **Graceful Degradation**: Invalid requests return error responses without affecting valid tokens

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/sandeep12222300/opd-token-allocation-engine.git
cd opd-token-allocation-engine
```

2. Build the project:
```bash
./mvnw clean install
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Running Tests

```bash
./mvnw test
```

## OPD Day Simulation

The application includes a built-in simulation that initializes on startup, demonstrating a typical OPD day with 3 doctors.

### Simulation Configuration

The simulation is automatically executed via `SimulationService` component:

```java
@Component
public class SimulationService implements CommandLineRunner {
    @Override
    public void run(String... args) {
        Doctor d1 = new Doctor("D1", 1.2);  // High efficiency (20% more capacity)
        d1.addSlot("9-10", 5);
        d1.addSlot("10-11", 5);
        
        Doctor d2 = new Doctor("D2", 1.0);  // Normal efficiency
        d2.addSlot("9-10", 4);
        d2.addSlot("10-11", 4);
        
        Doctor d3 = new Doctor("D3", 0.8);  // Lower efficiency (20% less capacity)
        d3.addSlot("9-10", 3);
        d3.addSlot("10-11", 3);
        
        // Doctors registered in InMemoryStore
    }
}
```

### Doctor Profiles

| Doctor | Efficiency Score | Base Capacity per Slot | Effective Capacity |
|--------|------------------|------------------------|-------------------|
| D1     | 1.2              | 5                      | 6 patients        |
| D2     | 1.0              | 4                      | 4 patients        |
| D3     | 0.8              | 3                      | 2-3 patients      |

**Total OPD Capacity**: ~24 patients across 2 time slots (9-10 AM, 10-11 AM)

### Simulating a Full OPD Day

To simulate a complete OPD day, send token requests to the API:

```bash
# Emergency patient to Doctor D1, 9-10 AM slot
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D1",
    "slotId": "9-10",
    "patientId": "P001",
    "source": "EMERGENCY"
  }'

# Paid appointment to Doctor D2
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D2",
    "slotId": "9-10",
    "patientId": "P002",
    "source": "PAID"
  }'

# Walk-in patient
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D3",
    "slotId": "10-11",
    "patientId": "P003",
    "source": "WALK_IN"
  }'
```

### Sample Simulation Scenario

**Scenario**: Busy morning with mixed patient types

1. **9:00 AM - Slot Opens**
   - 3 online bookings arrive (P001-P003) for D1
   - 2 walk-ins arrive (P004-P005) for D2
   - Result: All allocated (within capacity)

2. **9:15 AM - Emergency Arrives**
   - Emergency patient (P006) requests D1 slot
   - D1 slot full (6/6 allocated)
   - Lowest priority online booking (P003) moved to waitlist
   - Emergency allocated immediately

3. **9:30 AM - Paid Patient Arrives**
   - Paid patient (P007) requests D2 slot
   - D2 slot full (4/4 allocated)
   - Lowest priority walk-in (P005) moved to waitlist
   - Paid patient allocated

4. **9:45 AM - Cancellation**
   - Patient P001 cancels from D1
   - Patient P003 from waitlist automatically promoted

5. **10:00 AM - New Slot Opens**
   - Process repeats for 10-11 AM slot
   - Remaining waitlisted patients can request new slot

### Observing Simulation Results

The application logs provide real-time insights:

```
ALLOCATED → Token added
Allocated tokens: 6
Waiting queue: 0

REALLOCATED → Lower-priority token moved
Allocated tokens: 6
Waiting queue: 1

Token 550e8400... | Waiting(min): 15 | Effective Priority: 54
```

## Examples

### Example 1: Normal Allocation

**Request**:
```bash
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D1",
    "slotId": "9-10",
    "patientId": "P101",
    "source": "ONLINE"
  }'
```

**Response**:
```json
{
  "tokenId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "ALLOCATED",
  "reason": "Token allocated successfully within slot capacity"
}
```

### Example 2: Slot Full - Added to Waitlist

**Request**:
```bash
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D1",
    "slotId": "9-10",
    "patientId": "P102",
    "source": "WALK_IN"
  }'
```

**Response** (if slot full and no lower priority to preempt):
```json
{
  "tokenId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "status": "WAITLISTED",
  "reason": "Slot is full; token added to waiting queue"
}
```

### Example 3: High Priority Preemption

**Request**:
```bash
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D1",
    "slotId": "9-10",
    "patientId": "P103",
    "source": "EMERGENCY"
  }'
```

**Response** (if slot full but emergency has higher priority):
```json
{
  "tokenId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "status": "REALLOCATED_LOW_PRIORITY",
  "reason": "Lower-priority token was reallocated to the waiting queue based on fairness rules"
}
```

### Example 4: Token Cancellation

**Request**:
```bash
curl -X POST "http://localhost:8080/tokens/cancel?doctorId=D1&slotId=9-10&tokenId=a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

**Response**:
```
Token cancelled successfully by patient
```

### Example 5: Invalid Request

**Request**:
```bash
curl -X POST http://localhost:8080/tokens \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "D99",
    "slotId": "9-10",
    "patientId": "P104",
    "source": "ONLINE"
  }'
```

**Response**:
```json
{
  "tokenId": null,
  "status": "ERROR",
  "reason": "Doctor not found"
}
```

## Architecture Highlights

### Design Patterns Used

1. **Service Layer Pattern**: Separation of business logic (AllocationService) from controllers
2. **Repository Pattern**: InMemoryStore acts as data access layer
3. **Strategy Pattern**: Priority calculation encapsulated in PriorityCalculator
4. **DTO Pattern**: Data transfer objects for API communication

### Technology Stack

- **Framework**: Spring Boot 3.5.10
- **Language**: Java 17
- **Build Tool**: Maven
- **Data Structures**: Priority Queues (Min/Max Heaps)
- **Logging**: System.out (production should use SLF4J/Logback)

## Future Enhancements

1. **Persistence Layer**: Replace InMemoryStore with database (PostgreSQL/MySQL)
2. **Real-time Notifications**: WebSocket support for token status updates
3. **Multi-day Scheduling**: Support booking beyond current day
4. **Doctor Availability Management**: Handle breaks, holidays, emergency leaves
5. **Patient History Integration**: Consider previous visit patterns
6. **Analytics Dashboard**: Visual representation of allocation patterns
7. **SMS/Email Notifications**: Alert patients about token status
8. **Mobile App Integration**: RESTful API ready for mobile clients

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For questions or support, please open an issue in the GitHub repository.
