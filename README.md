# Annex Take-home Assignment

## Overview

Pretend we are selling a very simplified home insurance product that only offers dwelling and contents coverage.
Your task is to create a REST API that returns yearly premiums or declinations for a given home according to the provided rating tables and underwriting guidelines.
A declination is a refusal to offer a price given a specific input.

```
POST /api/homeowners_lite/quote
{
  // your JSON payload
}
```

The primary implementation language should be Scala 3. You can define your JSON request and response payloads as you see fit.
You can also use any external libraries.

## Requirements

### Fields to capture

We want to capture the following information in our rating API:

- risk address (i.e the address of the home being insured)
- policy effective date (i.e. when the policy takes effect)
- age of roof (must be a positive integer <= 100)
- year built (cannot be 100 years older than the effective date)
- dwelling limit (a positive integer <= 1M)
- dwelling deductible (an integer 1-10 representing the percentage from the dwelling limit)
- contents limit (a positive integer <= 250k)
- contents deductible (a positive integer rounded to the nearest thousand between 1k and 10k)

### Underwriting Guidelines

- Valid states are NJ, VA, TX, and FL. Any other state should return a decline
- Maximum dwelling limit is $1M
- Maximum contents limit is $250k
- Roofs older than 100 years are ineligible
- Risk that were built over 100 years before the policy effective date are ineligible

### Calculating Rates

We have included 2 rating tables: `wind_factors.csv` and `aop_factors.csv`. These will be used to calculate the premium
for the wind and AOP perils, respectively.

To calculate a premium for a given peril, start with a base rate per $100 of total insured value (TIV). In our case,
TIV = dwelling limit + contents limit. Then, we look up the appropriate factor for each input.
We then multiply the base rate and TIV by each factor. The total premium for the policy is the sum of wind premium and AOP premium.

NOTE: In the tables, the Year Built factor is actually the age of the home. So you will first need to calculate the age of the home
as of the effective date. You should round to the nearest whole year.

## Example Calculation

The following is an example of a full rate calculation.

Base rate per $100 of TIV: .50

| Coverage | Limit (Deductible) |
| -------- | ------------------ |
| Dwelling | 100k (5%)          |
| Contents | 20k (6k)           |

TIV = 120k

### Wind

| Factor and rate | Value |
| --------------- | ----- |
| Roof age rate   | 1.05  |
| Year Built      | .8    |
| 5% deductible   | 1     |

Wind premium = (.5) x 120k/100 x (1.05) x (.8) x (1) = $504

### AOP

Base rate per $100 of TIV: .50

| Factor and rate | Value |
| --------------- | ----- |
| Roof age rate   | 1.05  |
| Year Built      | .8    |
| 6k deductible   | .94   |

AOP premium = (.5) x 120k/100 x (1.05) x (.8) x (.94) = $473.76

### Total

Total = Wind + AOP = $504 + $473.76 = $977.76
