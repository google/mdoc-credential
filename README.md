# mdoc-credential

This repository contains a library that offers support for ISO 18013-5 mdoc via
`CredentialManager` present in Android 14 (U). An example app is also included.

## Usage

The library contains support for both providing and requesting credentials, and the example app
does both.

Releases are distributed through [Google Maven](https://maven.google.com/web/index.html?q=mdoc#com.google.android.mdoc:mdoc-credential).

### Relying Party

In order for an app to request a mdoc credential, it needs to create a `GetCredentialRequest` for
use with `CredentialManager` (via the [Jetpack library](https://developer.android.com/jetpack/androidx/releases/credentials)) which includes the appropriate options for mdoc. This is fairly
straightforward:

```kotlin
val option = GetMdocCredentialOption.create(
    handover = MdocHandover.ANDROID,
    nonce = nonce,
    publicKey = publicKey,
    documentType = MdocCredential.DOCUMENT_TYPE_MDL,
    requestedElements = elements,
    criticalElements = elements,
    retentionInDays = GetMdocCredentialOption.RETENTION_NONE
)

val request = GetCredentialRequest(listOf(option))
val credman = CredentialManager.create(context)
val cred = credman.getCredential(context, request).credential
```

`handover` identifies whether this request is coming from an app or a Web Browser. This affects how
the `SessionTranscript` (18013-5#9.1.5.1) is formed.  
`nonce` is any non-empty `ByteArray`  
`publicKey` is an `ECPublicKey` using `secp256r1`  
`requestedElements` is a `Set` of `MdocElement` containing the elements you want to be included
in the resulting credential.
`criticalElements` is a similar `Set` of `MdocElement`, but these are only used to narrow the
selections in the `CredentialManager` picker UI. It may be useful to include a more minimal set
of elements here to ensure that a credential request is successful. Of course, it's also fine to
use the same set of elements for both.  
  
A successful request returns a `MdocCredential`, which itself contains the HPKE-encrypted
`DeviceResponse` CBOR and the encapsulated sender key. The example app contains some code to do HPKE via
[Tink](https://developers.google.com/tink) and uses [cbor-java](https://github.com/c-rack/cbor-java)
to parse the `DeviceResponse`. This code or variants of it may find their way into the
`identity-credential` project eventually.  
  
An unsuccessful request will throw `GetCredentialException`.

### Provider

For mdoc, we want to use the registry feature of `CredentialManager`. This allows us to provide
credentials without the user needing to enable the service in the Android Settings, and also
avoids some potential privacy-related badness that can occur with other providers seeing
unrelated requests. This library has a helper, `MdocRegistry`, to make it easy to register credentials:

```kotlin
val SUPPORTED_ELEMENTS = setOf(
    MdocCredentialElement("family_name", MdocCredentialElement.NAMESPACE_MDL),
    MdocCredentialElement("issue_date", MdocCredentialElement.NAMESPACE_MDL),
    MdocCredentialElement("expiry_date", MdocCredentialElement.NAMESPACE_MDL),
    MdocCredentialElement("document_number", MdocCredentialElement.NAMESPACE_MDL),
    MdocCredentialElement("driving_privileges", MdocCredentialElement.NAMESPACE_MDL),
)

val registry = MdocRegistry(context)

val pendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, GetCredentialActivity::class.java),
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
)
        
registry.registerCredential(
    title = "Jane Doe's Driver's License",
    icon = Icon.createWithResource(context, android.R.drawable.ic_secure),
    documentType = MdocCredential.DOCUMENT_TYPE_MDL,
    supportedElements = SUPPORTED_ELEMENTS,
    pendingIntent = pendingIntent
)
```

The `pendingIntent` is used to launch the consent UI for your provider, so in the above example
you would replace `GetCredentialActivity` with your own.  
  
The `supportedElements` argument is the list of elements your provider is capable of returning
for a given credential. If all supported elements are not present here, a request may fail to match
and will not be presented to the user for selection.
  
As mentioned above, the `PendingIntent` references your own `Activity` that will be launched to
service incoming requests. This `Activity` will typically show the user what is being requested and
may also allow some modification of the response. Once it's determined that the user does want to
allow this credential to fulfill the request, the provider needs to encrypt the mdoc with the
public key present in the request, set this as the result of the `Activity`, and finally `finish()`
the `Activity`. Some example code to form a response is below:

```kotlin
val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)!!
val option =
    GetMdocCredentialOption.createFrom(request.credentialOptions[0] as GetCustomCredentialOption)
val hpke = MdocHpke(option.publicKey)
val handoverBytes = when (option.handover) {
    MdocHandover.ANDROID -> hpke.generateAndroidSessionTranscript(
        nonce = option.nonce,
        publicKey = option.publicKey,
        packageName = request.callingAppInfo.packageName
    )
    MdocHandover.BROWSER -> hpke.generateBrowserSessionTranscript(
        nonce = option.nonce,
        publicKey = option.publicKey,
        origin = request.callingAppInfo.origin!!
    )
}

val credential =
    MdocCredential(hpke.encrypt(cborResponseBytes, handoverBytes))
val response = GetCredentialResponse(credential)

val result = Intent()
PendingIntentHandler.setGetCredentialResponse(result, response)
setResult(Activity.RESULT_OK, result)
finish()
```

If the provider wants to abort the request, because the user has declined to consent to sharing the
credential or any other reason, it can return an error via `PendingIntentHandler.setGetCredentialException()`
and `finish()` the `Activity`.