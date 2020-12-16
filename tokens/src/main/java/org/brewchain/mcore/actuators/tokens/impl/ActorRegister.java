package org.brewchain.mcore.actuators.tokens.impl;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.mcore.actuators.tokens.impl20.Token20Impl;
import org.brewchain.mcore.actuators.tokens.impl721.Token721Impl;
import org.brewchain.mcore.handler.ActuactorHandler;
import org.brewchain.mcore.handler.MCoreServices;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@Data
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "tokens_actor_reg")
@Slf4j
public class ActorRegister implements ActorService {

	@ActorRequire(name = "MCoreServices", scope = "global")
	MCoreServices mcore;

	@ActorRequire(name = "bc_actuactor", scope = "global")
	ActuactorHandler actuactorHandler;

	@ActorRequire(name = "tokens_contract_da", scope = "global")
	TokenContractDataAccess tokenContractDAO;

	@Validate
	public void startup() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (actuactorHandler == null || mcore == null || tokenContractDAO == null
							|| !tokenContractDAO.isReady()) {
						if (tokenContractDAO == null) {
							log.debug("waiting actuator active...mcore=" + (mcore != null) + ",actuactorHandler="
									+ (actuactorHandler != null) + ",tokenContractDAO=null");
						} else {
							log.debug("waiting actuator active...mcore=" + (mcore != null) + ",actuactorHandler="
									+ (actuactorHandler != null) + ",tokenContractDAO.isReady="
									+ tokenContractDAO.isReady());

						}
						Thread.sleep(1000);
					}
					actuactorHandler.registerActutor(new Token20Impl(mcore, tokenContractDAO));
					actuactorHandler.registerActutor(new Token721Impl(mcore, tokenContractDAO));
					log.info("Token20-Actuator-Registered!");
				} catch (Exception e) {

					log.error("error in register token actuator", e);
				}
			}
		}).start();
	}
}