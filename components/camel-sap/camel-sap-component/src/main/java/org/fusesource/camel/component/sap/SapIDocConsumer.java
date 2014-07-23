/**
 * Copyright 2014 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.camel.component.sap;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.fusesource.camel.component.sap.model.idoc.Document;
import org.fusesource.camel.component.sap.util.IDocUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocDocumentIterator;
import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * Represents an SAP consumer receiving an IDoc (Intermediate Document) from in SAP. 
 * 
 * @author William Collins <punkhornsw@gmail.com>
 * 
 */
public class SapIDocConsumer extends DefaultConsumer implements JCoIDocHandler {

	private static final Logger LOG = LoggerFactory.getLogger(SapIDocConsumer.class);

	public SapIDocConsumer(Endpoint endpoint, Processor processor) {
		super(endpoint, processor);
	}

	@Override
	public SapIDocServerEndpoint getEndpoint() {
		return (SapIDocServerEndpoint) super.getEndpoint();
	}

	@Override
	public void handleRequest(JCoServerContext serverContext, IDocDocumentList idocDocumentList) {

		IDocDocumentIterator iter = idocDocumentList.iterator();
		while (iter.hasNext()) {
			try {
				IDocDocument idocDocument = iter.next();

				if (LOG.isDebugEnabled()) {
					LOG.debug("Handling IDoc document {}", idocDocument.toString());
				}


				Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOnly);

				// Create and populate document
				Document document = IDocUtil.createIDoc(getEndpoint().getServer().getIDocRepository(), idocDocumentList.getIDocType(), idocDocumentList.getIDocTypeExtension(), idocDocumentList.getSystemRelease(), idocDocumentList.getApplicationRelease());
				IDocUtil.extractIDocDocumentIntoDocument(idocDocument, document);
				
				// Populated exchange message
				Message message = exchange.getIn();
				message.setBody(document);

				// Process exchange
				getProcessor().process(exchange);
				
				JCoServerTIDHandler jcoServerTidHandler = serverContext.getServer().getTIDHandler();
				if (jcoServerTidHandler instanceof ServerTIDHandler) {
					((ServerTIDHandler)jcoServerTidHandler).execute(serverContext);
				}
				
			} catch (Exception e) {
				getExceptionHandler().handleException("Failed to process IDoc document", e);
			}
		}

	}
}
