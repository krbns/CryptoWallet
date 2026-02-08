# Crypto Wallet Test Assignment

Simple Android app built with Dynamic SDK.

Implemented 3 screens:
1. Login (Email OTP + modal bottom sheet verification)
2. Wallet Details (Sepolia + balance)
3. Send Transaction

## Architecture Description

Architecture is **MVVM + Repository + Hilt DI**.

1. UI layer:
- Jetpack Compose screens
- Material Design 3 components
- `StateFlow`-driven UI state
- feature-first structure:
  - `feature/login/ui`
  - `feature/walletdetails/ui`
  - `feature/send/ui`

2. ViewModel layer:
- `@HiltViewModel`
- business logic for login, wallet loading, and transaction sending
- loading/error/success states

3. Data layer (repositories):
- `core/data/repository/AuthRepository` for OTP and logout
- `core/data/repository/WalletRepository` for EVM wallet, Sepolia switch, balance
- `core/data/repository/TransactionRepository` for EVM send transaction

4. DI:
- Hilt modules in `core/di/AppModule.kt`
- app entry with `@HiltAndroidApp`

Navigation flow:
- `Login -> Wallet Details -> Send Transaction`
- OTP code verification is handled in Login via modal bottom sheet.

## How To Run

1. Open project root in Android Studio:
- `AndroidStudioProjects/CryptoWallet`

2. Create Dynamic project in [app.dynamic.xyz](https://app.dynamic.xyz)

3. Set your `environmentId` in:
- `CryptoWallet/ExampleApp/DynamicSDKExample/src/main/java/com/dynamic/sdk/example/MainActivity.kt`

4. Ensure redirect scheme matches:
- `redirectUrl` in `MainActivity.kt`
- `<data android:scheme="...">` in `CryptoWallet/ExampleApp/DynamicSDKExample/src/main/AndroidManifest.xml`

5. Sync Gradle and run `app` configuration on emulator/device.

6. (For sending tx) Fund your wallet with Sepolia ETH:
- [Google Faucet](https://cloud.google.com/application/web3/faucet/ethereum/sepolia)
- [Alchemy Faucet](https://www.alchemy.com/faucets/ethereum-sepolia)

## Screenshots (3 Screens)

Place screenshots in:
- `docs/screenshots/login.png`
- `docs/screenshots/wallet.png`
- `docs/screenshots/send.png`

Preview:

![Login](docs/screenshots/login.png)
![Wallet Details](docs/screenshots/wallet.png)
![Send Transaction](docs/screenshots/send.png)

## Assumptions Made

1. Dynamic environment has EVM wallet support enabled.
2. Sepolia network (`11155111`) is available in the Dynamic environment.
3. User can receive Email OTP and complete verification.
4. User has Sepolia ETH for transaction testing.
