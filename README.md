<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# CorDapp Training Project

This is an example CorDapp that simulates an environment where market data providers can sell access to their market
data using third-party redistributors. Subscribers can source data from multiple providers with complete transparency
to all parties.

# TODO
* Better discovery/distribution of data sets
* Implement the revoking of permissions
* Better billing support
* Updating prices and T&Cs
* On ledger payments

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running tests inside IntelliJ

We recommend editing your IntelliJ preferences so that you use the Gradle runner - this means that the quasar utils
plugin will make sure that some flags (like ``-javaagent`` - see below) are
set for you.

To switch to using the Gradle runner:

* Navigate to ``Build, Execution, Deployment -> Build Tools -> Gradle -> Runner`` (or search for `runner`)
  * Windows: this is in "Settings"
  * MacOS: this is in "Preferences"
* Set "Delegate IDE build/run actions to gradle" to true
* Set "Run test using:" to "Gradle Test Runner"

If you would prefer to use the built in IntelliJ JUnit test runner, you can run ``gradlew installQuasar`` which will
copy your quasar JAR file to the lib directory. You will then need to specify ``-javaagent:lib/quasar.jar``
and set the run directory to the project root directory for each test.

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Corda Kotlin Shell

There's a Corda Kotlin Shell extension script in scripts/market_data.kts. It contains some helper functions.

There's two functions that offer a step-through lifecycle:

- demo1() - Step through the setup and sharing of all states right up to permission and usage
- demo2() - After running demo1(), demo2 steps through the process of creating a 'paid-for' usage - i.e. one where the 
subscriber is already paying for access to data through another redistributor

To step through each flow in the functions, press the <return> key and wait for a response.