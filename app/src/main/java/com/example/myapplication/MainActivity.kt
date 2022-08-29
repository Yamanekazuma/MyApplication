package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import com.google.gson.JsonObject
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import okhttp3.Request

class MainActivity : AppCompatActivity() {
    private val SCOPES: List<String> = listOf("Files.Read")
    private val TAG: String? = MainActivity::class.simpleName
    private val AUTHORITY: String = "https://login.microsoftonline.com/46c67115-889e-4601-8e8d-6ff7e8bc6209"

    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

    private val signInButton: Button by lazy { findViewById(R.id.signIn) }
    private val signOutButton: Button by lazy { findViewById(R.id.clearCache) }
    private val callGraphApiInteractiveButton: Button by lazy { findViewById(R.id.callGraphInteractive) }
    private val callGraphApiSilentButton: Button by lazy { findViewById(R.id.callGraphSilent) }
    private val logTextView: TextView by lazy { findViewById(R.id.txt_log) }
    private val currentUserTextView: TextView by lazy { findViewById(R.id.current_user) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()

        PublicClientApplication.createSingleAccountPublicClientApplication(applicationContext,
            R.raw.auth_config_single_account, object: IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    application?.let {
                        mSingleAccountApp = it
                        loadAccount()
                    }
                }
                override fun onError(exception: MsalException?) {
                    exception?.let {
                        Log.e(TAG, exception.stackTraceToString())
                    //displayError(it)
                    } ?: run {
                        Log.e(TAG, "---NULL---")
                    }
                }
            })
    }

    private fun loadAccount() {
        mSingleAccountApp?.let {
            it.getCurrentAccountAsync(object: ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    updateUI(activeAccount)
                }
                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    currentAccount ?: run {
                        performOperationOnSignOut()
                    }
                }
                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
        }
    }

    private fun initializeUI() {
        signInButton.setOnClickListener {
            mSingleAccountApp?.let {
                val params: SignInParameters = SignInParameters.builder()
                    .withActivity(this)
                    .withLoginHint(null)
                    .withScopes(SCOPES)
                    .withPrompt(null)
                    .withCallback(getAuthInteractiveCallback())
                    .build()
                it.signIn(params)
            }
        }

        signOutButton.setOnClickListener {
            mSingleAccountApp?.signOut(object: ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    updateUI(null)
                    performOperationOnSignOut()
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
        }

        callGraphApiInteractiveButton.setOnClickListener {
            mSingleAccountApp?.let {
                val params = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(this)
                    .withScopes(SCOPES)
                    .withCallback(getAuthInteractiveCallback())
                    .build()
                it.acquireToken(params)
            }
        }

        callGraphApiSilentButton.setOnClickListener {
            mSingleAccountApp?.let {
                val params = AcquireTokenSilentParameters.Builder()
                    .withScopes(SCOPES)
                    .fromAuthority(AUTHORITY)
                    .withCallback(getAuthSilentCallback())
                    .build()
                it.acquireTokenSilentAsync(params)
            }
        }
    }

    private fun performOperationOnSignOut() {
        val signOutText = "Signed Out."
        currentUserTextView.text = ""
        Toast.makeText(applicationContext, signOutText, Toast.LENGTH_SHORT)
            .show()
    }

    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
        return object: SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authenticationResult?.let {
                    Log.d(TAG, "Successfully authenticated")
                    callGraphAPI(it)
                }
            }
            override fun onError(exception: MsalException?) {
                exception?.let {
                    Log.d(TAG, "Authentication failed: $it")
                    displayError(it)
                }
            }
        }
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object: AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authenticationResult?.let {
                    Log.d(TAG, "Successfully authenticated")
                    updateUI(it.account)
                    callGraphAPI(it)
                }
            }
            override fun onError(exception: MsalException?) {
                exception?.let {
                    Log.d(TAG, "Authentication failed: $it")
                    displayError(it)
                }
            }
            override fun onCancel() {
                Log.d(TAG, "User cancelled login.")
            }
        }
    }

    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        /*
        val accessToken: String  = authenticationResult.accessToken

        val graphClient: GraphServiceClient<Request> = GraphServiceClient.builder()
            .authenticationProvider {

            }
            .buildClient()
        val graphClient: IGraphServiceClient = GraphServiceClient.builder()
            .authenticationProvider {
                Log.d(TAG, "Authenticating request," + it.requestUrl)
                it.addHeader("Authorization", "Bearer $accessToken")
            }
            .buildClient()
        graphClient.me()
            .drive()
            .buildRequest()
            .get(object: ICallback<Drive> {
                override fun success(result: Drive?) {
                    result?.let {
                        Log.d(TAG, "Found Drive " + it.id)
                        displayGraphResult(it.rawObject)
                    }
                }
                override fun failure(ex: ClientException?) {
                    ex?.let {
                        displayError(it)
                    }
                }
            })
         */
    }

    private fun displayGraphResult(graphResponse: JsonObject) {
        logTextView.text = graphResponse.toString()
    }

    private fun displayError(exception: Exception) {
        logTextView.text = exception.toString()
    }

    private fun updateUI(account: IAccount?) {
        if (account != null) {
            signInButton.isEnabled = false
            signOutButton.isEnabled = true
            callGraphApiInteractiveButton.isEnabled = true
            callGraphApiSilentButton.isEnabled = true
            currentUserTextView.text = "TEST_MESSAGE" // account.username
            Log.e(TAG, "currentUser is shown:" + currentUserTextView.isShown)
            Log.e(TAG, "currentUser is invisible:" + currentUserTextView.isInvisible)
        } else {
            signInButton.isEnabled = true
            signOutButton.isEnabled = false
            callGraphApiInteractiveButton.isEnabled = false
            callGraphApiSilentButton.isEnabled = false
            currentUserTextView.text = ""
            logTextView.text = ""
        }
    }
}