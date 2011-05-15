/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.jobs;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.torquebox.core.app.RubyApplicationMetaData;
import org.torquebox.core.runtime.PoolMetaData;

/**
 * <pre>
 * Stage: DESCRIBE
 *    In: EnvironmentMetaData, PoolMetaData, ScheduledJobMetaData
 *   Out: PoolMetaData
 * </pre>
 * 
 * Ensures that pool metadata for jobs is available
 */
public class JobsRuntimePoolProcessor implements DeploymentUnitProcessor {

	public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
		DeploymentUnit unit = phaseContext.getDeploymentUnit();

		List<ScheduledJobMetaData> jobMetaData = unit.getAttachmentList( ScheduledJobMetaData.ATTACHMENTS_KEY ); 
        if (jobMetaData.isEmpty()) {
            return;
        }
        
        List<PoolMetaData> allMetaData = unit.getAttachmentList( PoolMetaData.ATTACHMENTS_KEY );
        PoolMetaData jobsPool = PoolMetaData.extractNamedMetaData( allMetaData, "jobs" );
        
        if (jobsPool == null) {
            RubyApplicationMetaData envMetaData = unit.getAttachment( RubyApplicationMetaData.ATTACHMENT_KEY );
            boolean devMode = envMetaData != null && envMetaData.isDevelopmentMode();
            jobsPool = devMode ? new PoolMetaData( "jobs", 1, 2 ) : new PoolMetaData( "jobs" );
            unit.addToAttachmentList( PoolMetaData.ATTACHMENTS_KEY, jobsPool );
        }
    }
    
    public void undeploy(DeploymentUnit unit) {
		
    }	

}
