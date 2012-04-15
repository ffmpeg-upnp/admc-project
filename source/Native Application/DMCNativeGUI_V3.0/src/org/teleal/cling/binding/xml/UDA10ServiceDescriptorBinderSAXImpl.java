/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.binding.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.teleal.cling.binding.staging.MutableAction;
import org.teleal.cling.binding.staging.MutableActionArgument;
import org.teleal.cling.binding.staging.MutableAllowedValueRange;
import org.teleal.cling.binding.staging.MutableService;
import org.teleal.cling.binding.staging.MutableStateVariable;
import org.teleal.cling.binding.xml.Descriptor.Service.ATTRIBUTE;
import org.teleal.cling.binding.xml.Descriptor.Service.ELEMENT;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariableEventDetails;
import org.teleal.cling.model.types.CustomDatatype;
import org.teleal.cling.model.types.Datatype;
import org.teleal.common.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation based on JAXP SAX.
 * 
 * @author Christian Bauer
 */
public class UDA10ServiceDescriptorBinderSAXImpl extends UDA10ServiceDescriptorBinderImpl {

	private static Logger log = Logger.getLogger(ServiceDescriptorBinder.class.getName());

	@Override
	public <S extends Service> S describe(S undescribedService, String descriptorXml) throws DescriptorBindingException, ValidationException {

		if (descriptorXml == null || descriptorXml.length() == 0) {
			throw new DescriptorBindingException("Null or empty descriptor");
		}

		try {
			log.fine("Reading service from XML descriptor");

			SAXParser parser = new SAXParser();

			MutableService descriptor = new MutableService();

			hydrateBasic(descriptor, undescribedService);

			new RootHandler(descriptor, parser);

			parser.parse(new InputSource(
			// TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML element, need to trim()
					new StringReader(descriptorXml.trim())));

			// Build the immutable descriptor graph
			return (S) descriptor.build(undescribedService.getDevice());

		} catch (ValidationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new DescriptorBindingException("Could not parse service descriptor: " + ex.toString(), ex);
		}
	}

	protected static class RootHandler extends ServiceDescriptorHandler<MutableService> {

		public RootHandler(MutableService instance, SAXParser parser) {
			super(instance, parser);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

			/*
			 * if (element.equals(SpecVersionHandler.EL)) { MutableUDAVersion udaVersion = new MutableUDAVersion(); getInstance().udaVersion = udaVersion; new
			 * SpecVersionHandler(udaVersion, this); }
			 */

			if (element.equals(ActionListHandler.EL)) {
				List<MutableAction> actions = new ArrayList();
				getInstance().actions = actions;
				new ActionListHandler(actions, this);
			}

			if (element.equals(StateVariableListHandler.EL)) {
				List<MutableStateVariable> stateVariables = new ArrayList();
				getInstance().stateVariables = stateVariables;
				new StateVariableListHandler(stateVariables, this);
			}

		}
	}

	/*
	 * protected static class SpecVersionHandler extends ServiceDescriptorHandler<MutableUDAVersion> {
	 * 
	 * public static final ELEMENT EL = ELEMENT.specVersion;
	 * 
	 * public SpecVersionHandler(MutableUDAVersion instance, ServiceDescriptorHandler parent) { super(instance, parent); }
	 * 
	 * @Override public void endElement(ELEMENT element) throws SAXException { switch (element) { case major: getInstance().major =
	 * Integer.valueOf(getCharacters()); break; case minor: getInstance().minor = Integer.valueOf(getCharacters()); break; } }
	 * 
	 * @Override public boolean isLastElement(ELEMENT element) { return element.equals(EL); } }
	 */

	protected static class ActionListHandler extends ServiceDescriptorHandler<List<MutableAction>> {

		public static final ELEMENT EL = ELEMENT.actionList;

		public ActionListHandler(List<MutableAction> instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
			if (element.equals(ActionHandler.EL)) {
				MutableAction action = new MutableAction();
				getInstance().add(action);
				new ActionHandler(action, this);
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class ActionHandler extends ServiceDescriptorHandler<MutableAction> {

		public static final ELEMENT EL = ELEMENT.action;

		public ActionHandler(MutableAction instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
			if (element.equals(ActionArgumentListHandler.EL)) {
				List<MutableActionArgument> arguments = new ArrayList();
				getInstance().arguments = arguments;
				new ActionArgumentListHandler(arguments, this);
			}
		}

		@Override
		public void endElement(ELEMENT element) throws SAXException {
			switch (element) {
			case name:
				getInstance().name = getCharacters();
				break;
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class ActionArgumentListHandler extends ServiceDescriptorHandler<List<MutableActionArgument>> {

		public static final ELEMENT EL = ELEMENT.argumentList;

		public ActionArgumentListHandler(List<MutableActionArgument> instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
			if (element.equals(ActionArgumentHandler.EL)) {
				MutableActionArgument argument = new MutableActionArgument();
				getInstance().add(argument);
				new ActionArgumentHandler(argument, this);
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class ActionArgumentHandler extends ServiceDescriptorHandler<MutableActionArgument> {

		public static final ELEMENT EL = ELEMENT.argument;

		public ActionArgumentHandler(MutableActionArgument instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(ELEMENT element) throws SAXException {
			switch (element) {
			case name:
				getInstance().name = getCharacters();
				break;
			case direction:
				getInstance().direction = ActionArgument.Direction.valueOf(getCharacters().toUpperCase());
				break;
			case relatedStateVariable:
				getInstance().relatedStateVariable = getCharacters();
				break;
			case retval:
				getInstance().retval = true;
				break;
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class StateVariableListHandler extends ServiceDescriptorHandler<List<MutableStateVariable>> {

		public static final ELEMENT EL = ELEMENT.serviceStateTable;

		public StateVariableListHandler(List<MutableStateVariable> instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
			if (element.equals(StateVariableHandler.EL)) {
				MutableStateVariable stateVariable = new MutableStateVariable();

				String sendEventsAttributeValue = attributes.getValue(ATTRIBUTE.sendEvents.toString());
				stateVariable.eventDetails = new StateVariableEventDetails(sendEventsAttributeValue != null
						&& sendEventsAttributeValue.toUpperCase().equals("YES"));

				getInstance().add(stateVariable);
				new StateVariableHandler(stateVariable, this);
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class StateVariableHandler extends ServiceDescriptorHandler<MutableStateVariable> {

		public static final ELEMENT EL = ELEMENT.stateVariable;

		public StateVariableHandler(MutableStateVariable instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
			if (element.equals(AllowedValueListHandler.EL)) {
				List<String> allowedValues = new ArrayList();
				getInstance().allowedValues = allowedValues;
				new AllowedValueListHandler(allowedValues, this);
			}

			if (element.equals(AllowedValueRangeHandler.EL)) {
				MutableAllowedValueRange allowedValueRange = new MutableAllowedValueRange();
				getInstance().allowedValueRange = allowedValueRange;
				new AllowedValueRangeHandler(allowedValueRange, this);
			}
		}

		@Override
		public void endElement(ELEMENT element) throws SAXException {
			switch (element) {
			case name:
				getInstance().name = getCharacters();
				break;
			case dataType:
				String dtName = getCharacters();
				Datatype.Builtin builtin = Datatype.Builtin.getByDescriptorName(dtName);
				getInstance().dataType = builtin != null ? builtin.getDatatype() : new CustomDatatype(dtName);
				break;
			case defaultValue:
				getInstance().defaultValue = getCharacters();
				break;
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class AllowedValueListHandler extends ServiceDescriptorHandler<List<String>> {

		public static final ELEMENT EL = ELEMENT.allowedValueList;

		public AllowedValueListHandler(List<String> instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(ELEMENT element) throws SAXException {
			switch (element) {
			case allowedValue:
				getInstance().add(getCharacters());
				break;
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class AllowedValueRangeHandler extends ServiceDescriptorHandler<MutableAllowedValueRange> {

		public static final ELEMENT EL = ELEMENT.allowedValueRange;

		public AllowedValueRangeHandler(MutableAllowedValueRange instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(ELEMENT element) throws SAXException {
			try {
				switch (element) {
				case minimum:
					getInstance().minimum = Long.valueOf(getCharacters());
					break;
				case maximum:
					getInstance().maximum = Long.valueOf(getCharacters());
					break;
				case step:
					getInstance().step = Long.valueOf(getCharacters());
					break;
				}
			} catch (Exception ex) {
				// Ignore
			}
		}

		@Override
		public boolean isLastElement(ELEMENT element) {
			return element.equals(EL);
		}
	}

	protected static class ServiceDescriptorHandler<I> extends SAXParser.Handler<I> {

		public ServiceDescriptorHandler(I instance) {
			super(instance);
		}

		public ServiceDescriptorHandler(I instance, SAXParser parser) {
			super(instance, parser);
		}

		public ServiceDescriptorHandler(I instance, ServiceDescriptorHandler parent) {
			super(instance, parent);
		}

		public ServiceDescriptorHandler(I instance, SAXParser parser, ServiceDescriptorHandler parent) {
			super(instance, parser, parent);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			ELEMENT el = ELEMENT.valueOrNullOf(localName);
			if (el == null)
				return;
			startElement(el, attributes);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			ELEMENT el = ELEMENT.valueOrNullOf(localName);
			if (el == null)
				return;
			endElement(el);
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			ELEMENT el = ELEMENT.valueOrNullOf(localName);
			return el != null && isLastElement(el);
		}

		public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

		}

		public void endElement(ELEMENT element) throws SAXException {

		}

		public boolean isLastElement(ELEMENT element) {
			return false;
		}
	}

}