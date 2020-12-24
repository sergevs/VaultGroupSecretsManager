package vault.gscm
// https://github.com/BetterCloud/vault-java-driver/
import com.bettercloud.vault.*
import com.bettercloud.vault.response.*
import java.util.logging.Logger
import org.yaml.snakeyaml.Yaml

class vaultPathSecrets {
  private String source
  private String[] targets
  private Map<String, String> secretsData
  private String[] secrets
  private static Logger logger
  static {
    System.setProperty('java.util.logging.SimpleFormatter.format',
      '[%1$tF %1$tT] [%4$-7s] %5$s %n');
    logger = Logger.getLogger(vaultPathSecrets.class.getCanonicalName())
  }

  vaultPathSecrets (String source, String[] targets, String[] secrets) {
    this.source = source
    this.targets = targets
    this.secrets = secrets
  }

  public String getSource () {
    return source
  }

  public void collectSecrets(Vault vault) {
    def vaultResponse = vault.logical().read(this.source)
    def readStatus = vaultResponse.getRestResponse().getStatus()
    if (readStatus != 200) {
      logger.warning("${this.source} read failed with status: ${readStatus}, ignoring. Check configuration")
      return
    } else {
      logger.info("Read source ${this.source}")
    }

    if(this.secrets) {
      this.secretsData = new HashMap<String, String>()
      this.secrets.each { secret ->
        def secretValue = vaultResponse.getData().get(secret)
        if(secretValue) {
          this.secretsData.put(secret, secretValue)
        } else {
          logger.warning("Fetch '${secret}' at '${source}' returns null, ignoring. Check configuration")
        }
      } 
    } else {
      this.secretsData = vaultResponse.getData()
    }
  }
}

class vaultGroupSecretsManager {
  private Vault vault
  private targetsMap = [:]
  private static Logger logger
  static {
    System.setProperty('java.util.logging.SimpleFormatter.format',
      '[%1$tF %1$tT] [%4$-7s] %5$s %n');
    logger = Logger.getLogger(vaultGroupSecretsManager.class.getCanonicalName())
  }

  vaultGroupSecretsManager(Vault vault, String config) {
    this.vault = vault
    def yamlParser = new Yaml()
    def yamlMap = yamlParser.load((config as File).text)
    def copySecretsMap = []

    // load paths configuration to objects
    yamlMap['secretMap'].each { mapItem ->
      logger.info("Loading configuration for ${mapItem['source']}")
      copySecretsMap.add(new vaultPathSecrets( 
          mapItem['source'] as String, 
          mapItem['targets'] as String[],
          mapItem['secrets'] as String[]
        )
      )
    }
    // read secrets
    copySecretsMap.each { mapItem ->
      mapItem.collectSecrets(vault)
      mapItem.targets.each { targetItem ->
        if(this.targetsMap[targetItem]) {
          this.targetsMap[targetItem] += mapItem.secretsData
        } else {
          this.targetsMap[targetItem] = mapItem.secretsData
        }
      }
    } 
  }
  
  public boolean isUpdateRequired(String targetPath) {
    logger.info("Processing ${targetPath} target")
    def vaultData = vault.logical()
                      .read(targetPath)
                      .getData()
    def isTargetDataEquals = vaultData.equals(this.targetsMap[targetPath])
    if (isTargetDataEquals) {
      logger.info("${targetPath} data at desired state")
    } else {
      logger.info("${targetPath} update is required")
    }
    return !isTargetDataEquals  
  }
  
  public updateTargetsPath() {
    this.targetsMap.each { secretPath, secretData ->
      if (isUpdateRequired(secretPath)) {
        def writeStatus = vault.logical()
                        .write(secretPath, secretData)
                        .getRestResponse()
                        .getStatus()
        if (writeStatus == 200) {
          logger.info("${secretPath} updated")
        } else {
          logger.severe("${secretPath} update failed with status ${writeStatus}")
        }
      }
    }
  }
  
  public Map getTargetsMap () {
    return targetsMap
  }
}
