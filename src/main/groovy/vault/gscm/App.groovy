package vault.gscm
import com.bettercloud.vault.*

/* 
environment variables:
  VAULT_ADDR FQDN or IP address of vautl server
  VAULT_TOKEN if set, authenticate with token
  VAULT_APPROLE_ID if set, authenticate with app role
  VAULT_APPROLE_SECRET required if VAULT_APPROLE_ID is specified
  VAULT_K8S_ROLE if set, authenticate with kubernetes
  VAULT_K8S_TOKEN jwt token, required if VAULT_K8S_ROLE is specified
  VAULT_GSCM_CONFIG configuration file name, default to gscm-config.yaml
*/

// configure vault connection and authentication
// https://github.com/BetterCloud/vault-java-driver/blob/master/src/main/java/com/bettercloud/vault/api/Auth.java
vaultConfig = new VaultConfig().build()
vault = new Vault(vaultConfig, 2)

if (System.getenv('VAULT_TOKEN')) {
  println 'INFO: Authenticating with token'
} else if (System.getenv('VAULT_APPROLE_ID')) {
  println 'INFO: Authenticating with app role'
  vaultAppRoleId = System.getenv('VAULT_APPROLE_ID')
  vaultAppRoleSecret = System.getenv('VAULT_APPROLE_SECRET')
  token = vault.auth()
              .loginByAppRole(vaultAppRoleId, vaultAppRoleSecret)
              .getAuthClientToken()
  vaultConfig.token(token).build()
} else if (System.getenv('VAULT_K8S_ROLE')) {
  println 'INFO: Authenticating with kubernetes'
  k8sRole = System.getenv('VAULT_K8S_ROLE')
  k8sToken =  System.getenv('VAULT_K8S_TOKEN')
  token = vault.auth()
              .loginByJwt('kubernetes', k8sRole, k8sToken)
              .getAuthClientToken()
  vaultConfig.token(token).build()
} else {
  System.err.println 'ERROR: No authentication method found'
  System.exit(1)
}

// configure group secrets manager
gscmConfig = System.getenv('VAULT_GSCM_CONFIG') ?: 'gscm-config.yaml'

groupSecretsManager = new vaultGroupSecretsManager(vault, gscmConfig)
groupSecretsManager.updateTargetsPath()
