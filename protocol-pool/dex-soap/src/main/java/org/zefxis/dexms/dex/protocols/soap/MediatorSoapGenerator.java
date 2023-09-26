package org.zefxis.dexms.dex.protocols.soap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zefxis.dexms.dexprotocols.gidl2wsdl.GidlToWSDL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.SingleStreamCodeWriter;


import org.zefxis.dexms.dex.protocols.rest.RestResponseBuilder;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Scope;
import org.zefxis.dexms.dex.protocols.generators.MediatorSubcomponentGenerator;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;
import org.zefxis.dexms.tools.logger.GLog;
import org.zefxis.dexms.tools.logger.Logger;


// TODO: refactor, clean & clear as much as possible...
public class MediatorSoapGenerator extends MediatorSubcomponentGenerator {
	
	private final String classComment = "This class was generated by the CHOReVOLUTION BindingComponent Generator using com.sun.codemodel 2.6";
	private Logger logger = GLog.initLogger();
	public MediatorSoapGenerator(GmServiceRepresentation serviceRepresentation, MediatorConfiguration bcConfiguration) {
		super(serviceRepresentation, bcConfiguration);
		
	}

	private void includePackageReference(String attrType, JCodeModel codeModel){
		int firstBracket = attrType.indexOf("<");
		if (firstBracket != -1){
			
			JClass dataClass = codeModel.ref(attrType.substring(0, firstBracket));
			includePackageReference(attrType.substring(firstBracket + 1, attrType.length() - 1), codeModel);
		}

	}

	public void generateWSDL(){
		
		GidlToWSDL gidlToWSDL = new GidlToWSDL(bcConfiguration);
		gidlToWSDL.generateWSDL();
		
		
	}


	@Override
	protected void generatePojo(final Data<?> definition) {
		
		JCodeModel codeModel = new JCodeModel();
		JDefinedClass definitionClass = this.generateDefinedClass(codeModel, definition.getClassName());
		this.addComment(definitionClass, this.classComment);

		definitionClass.annotate(javax.xml.bind.annotation.XmlAccessorType.class).param("value",
				javax.xml.bind.annotation.XmlAccessType.FIELD);

		definitionClass.annotate(javax.xml.bind.annotation.XmlRootElement.class).param("name",
				definition.getClassName());

		for (Data<?> attr : definition.getAttributes()){

			JFieldVar attrField = null;
			if (attr.getClassName().indexOf("<") != -1) {
				
				JClass ListClass = null;
				JClass argClass = codeModel.ref(attr.getClassName().substring(attr.getClassName().indexOf("<") + 1,
						attr.getClassName().length() - 1));
				ListClass = codeModel.ref(List.class).narrow(argClass);
				attrField = definitionClass.field(JMod.PRIVATE, ListClass, attr.getName());
				attrField.mods().setFinal(false);
				// includePackageReference(attr.getClassName(), codeModel);
			} else {
				attrField = generateAttribute(codeModel, definitionClass, codeModel.directClass(attr.getClassName()),
						attr.getName(), false);
			}
			JAnnotationUse attrAnnotation = attrField.annotate(codeModel.ref("javax.xml.bind.annotation.XmlElement"));
			attrAnnotation.param("name", attr.getName());
			attrAnnotation.param("required", attr.isRequired());

			// generate getters
			JMethod getterMethod =  definitionClass.method(JMod.PUBLIC, attrField.type(), "get" + attr.getName());
			getterMethod.annotate(JsonProperty.class).param("value", attr.getName());
			getterMethod.body()._return(attrField);
			
			// generate setters
			JMethod setterMethod = definitionClass.method(JMod.PUBLIC, codeModel.VOID, "set" + attr.getName());
			setterMethod.param(attrField.type(), attr.getName());
			setterMethod.body().assign(JExpr._this().ref(attr.getName()), JExpr.ref(attr.getName()));

			// if complex type, generate its definition
			if (!attr.isPrimitiveType()) {
				
				this.generatePojo(attr);
			}
		}

		this.buildGeneratedClass(codeModel);
	}
	
	@Override
	protected void generateEndpoint(){
		
		JCodeModel codeModel = new JCodeModel();
		JDefinedClass webService = generateDefinedClass(codeModel, this.bcConfiguration.getServiceName());

		this.addComment(webService, this.classComment);
		this.addAnnotations(codeModel, webService, "javax.jws.WebService");
		
		JAnnotationUse bindingAnnotation = webService.annotate(codeModel.ref("javax.jws.soap.SOAPBinding"));
		
		JClass apiClass = codeModel.ref(MediatorGmSubcomponent.class);
		JFieldVar apiField = generateAttribute(codeModel, webService, apiClass, "apiRef", true);

		JMethod constructor = webService.constructor(JMod.PUBLIC);
		constructor.param(apiClass, "apiRef");
		constructor.body().assign(JExpr._this().ref(apiField), apiField);

		Collection<Operation> operations = this.componentDescription.getOperations();
		for(Operation operation : operations){
			
			this.buildOperation(operation, webService, codeModel);
		}
		this.buildGeneratedClass(codeModel);
		
	}

	private JDefinedClass generateDefinedClass(final JCodeModel codeModel, final String className) {
		JDefinedClass definedClass = null;
		try {
			
			definedClass = codeModel._class(this.bcConfiguration.getTargetNamespace() + "." + className);
		
		} catch (JClassAlreadyExistsException e){
			
			e.printStackTrace();
		}
		return definedClass;
	}

	private void addAnnotations(final JCodeModel codeModel, final JDefinedClass definedClass,
			final String annotationClassRef) {
		JAnnotationUse webServiceAnnotation = definedClass.annotate(codeModel.ref(annotationClassRef));
		webServiceAnnotation.param("serviceName", bcConfiguration.getServiceName());
		webServiceAnnotation.param("targetNamespace", bcConfiguration.getTargetNamespace());
		
	}

	private JFieldVar generateAttribute(final JCodeModel codeModel, final JDefinedClass definedClass,
		
	    final Class<?> attrClass, final String attrName, final Boolean isFinal) {
		return this.generateAttribute(codeModel, definedClass, codeModel.ref(attrClass), attrName, isFinal);
	}

	private JFieldVar generateAttribute(final JCodeModel codeModel, final JDefinedClass definedClass,
			final JClass attrClass, final String attrName, final Boolean isFinal) {
		
		JFieldVar attrField = definedClass.field(JMod.PRIVATE, attrClass, attrName);
		attrField.mods().setFinal(isFinal);
		return attrField;
	}

	private void addComment(final JDefinedClass definedClass, final String comment) {
		definedClass.javadoc().add(comment);
	}

	private void buildGeneratedClass(final JCodeModel codeModel) {
		// TESTING PURPOSE
		// Write the generated class to the console output
		if (debug) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				codeModel.build(new SingleStreamCodeWriter(out));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.i(this.getClass().getName().toString(), out.toString());
			
		}

		try {
			
			codeModel.build(new File(this.bcConfiguration.getGeneratedCodePath()));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void buildOperation(final Operation operation, final JDefinedClass definedClass,
			final JCodeModel codeModel){
		JMethod method = null;
		int postDataSize = operation.getPostDatas().size();
		
		if (postDataSize == 0) {
			
			method = definedClass.method(JMod.PUBLIC, void.class, operation.getScope().getName());
			
		} else {
			
		    String codeModel_directClass = "OUTPUTDATATYPE";
			if(postDataSize == 1){
				
				String directClassTmp = codeModel_directClass;
				codeModel_directClass = operation.getPostDatas().get(postDataSize-1).getClassName();
				if(codeModel_directClass.equals("List<"+directClassTmp+">")){
					
					JClass importedClass= codeModel.ref(java.util.List.class)
							                        .narrow(codeModel.ref(directClassTmp));
					method = definedClass.method(JMod.PUBLIC, importedClass,operation.getScope().getName());
					
				}else{
					
					method = definedClass.method(JMod.PUBLIC, codeModel.directClass(codeModel_directClass),operation.getScope().getName());
				}
			}
			
			
			
		}

		JBlock methodBlock = method.body();

		method.annotate(codeModel.ref("javax.jws.WebMethod"));

		JClass dataListClass = codeModel.ref(List.class).narrow(codeModel.ref(Data.class).narrow(codeModel.wildcard()));
		JVar dataList = methodBlock.decl(dataListClass, "datas");
		dataList.init(JExpr
				._new(codeModel.ref(ArrayList.class).narrow(codeModel.ref(Data.class).narrow(codeModel.wildcard()))));

		this.wrapOperationParams(method, codeModel, operation.getGetDatas(), dataList);
		
		
		JClass serviceRepresentationClass = codeModel.ref(GmServiceRepresentation.class);
		JVar serviceRepresentation = methodBlock.decl(serviceRepresentationClass, "serviceRepresentation");
		methodBlock.assign(serviceRepresentation, JExpr._this().ref(definedClass.fields().get("apiRef")).invoke("getGmServiceRepresentation"));
		
		JClass scopeClass = codeModel.ref(Scope.class);
		JVar scope = methodBlock.decl(scopeClass, "scope");
		methodBlock.assign(scope,serviceRepresentation.invoke("getOperation")
                .arg( operation.getScope().getName()).invoke("getScope"));
		
		
		JInvocation gmInvocation = this.generateGmInvokation(operation, definedClass, scope).arg(dataList);
		JClass responseBuilderClass = codeModel.ref(RestResponseBuilder.class);
		JVar responseBuilder = methodBlock.decl(responseBuilderClass, "responseBuilder",JExpr._new(responseBuilderClass));
		
		if (operation.getOperationType() == OperationType.ONE_WAY) {
			methodBlock.add(gmInvocation);
		} else {
			
			
			String rootClass = "OUTPUTDATATYPE";
			String classToMap = "OUTPUTDATATYPE";
			String serialized = "serializedOUTPUTDATATYPE";
			if(postDataSize == 1){
				
				rootClass = operation.getPostDatas().get(postDataSize-1).getClassName();
				serialized = "serialized" + operation.getPostDatas().get(postDataSize-1).getName();
				
			}
			if(rootClass.startsWith("List<")){ // Unmarshall in the case of Json Array response
				
				JClass mapperClass = codeModel.ref(com.fasterxml.jackson.databind.ObjectMapper.class);
				JClass typeFactoryClass = codeModel.ref(com.fasterxml.jackson.databind.type.TypeFactory.class);
				JClass collectionTypeClass = codeModel.ref(com.fasterxml.jackson.databind.type.CollectionType.class);
				
				JVar mapper = methodBlock.decl(mapperClass,"mapper",JExpr._new(mapperClass));
				JVar typeFactory = methodBlock.decl(typeFactoryClass, "typeFactory");
				JVar collectionType = methodBlock.decl(collectionTypeClass, "collectionType");
				
				
				methodBlock.assign(typeFactory, mapper.invoke("getTypeFactory"));
				methodBlock.assign(collectionType,typeFactory.invoke("constructCollectionType")
						                                  .arg(codeModel.directClass("java.util.List").dotclass())
						                                  .arg(codeModel.directClass(classToMap).dotclass()));
				
				// unmarshal the serialized object
				JClass stringClass = codeModel.ref(String.class);
				JVar serializedResponse = methodBlock.decl(stringClass, serialized);
				serializedResponse.init(gmInvocation);
				
				JInvocation responseBuilderInvocation = responseBuilder.invoke("unmarshalObject")
						.arg(operation.getPostDatas().get(postDataSize-1).getMediaTypeAsString()).arg(serializedResponse)
						.arg(collectionType);

				methodBlock._return(responseBuilderInvocation);
				
			}
			else{  // Unmarshall in the case of Json Object response
				
				
				// unmarshal the serialized object
				JClass stringClass = codeModel.ref(String.class);
				JVar serializedResponse = methodBlock.decl(stringClass, serialized);
				serializedResponse.init(gmInvocation);
				
				JInvocation responseBuilderInvocation = responseBuilder.invoke("unmarshalObject")
						.arg(operation.getPostDatas().get(postDataSize-1).getMediaTypeAsString()).arg(serializedResponse)
						.arg(codeModel.directClass(rootClass).dotclass());

				methodBlock._return(responseBuilderInvocation);
			}
			
		}
	}
	
	private JInvocation generateGmInvokation(Operation operation, JDefinedClass definedClass, JVar scope) {
		JInvocation apiInvocation = null;
		switch (operation.getOperationType()) {
		case ONE_WAY:
			// TODO logs
			apiInvocation = JExpr._this().ref(definedClass.fields().get("apiRef")).invoke("mgetOneway")
					.arg(scope);
			break;
		case TWO_WAY_SYNC:
			apiInvocation = JExpr._this().ref(definedClass.fields().get("apiRef")).invoke("mgetTwowaySync")
					.arg(scope);
			break;
		case TWO_WAY_ASYNC:
			apiInvocation = JExpr._this().ref(definedClass.fields().get("apiRef")).invoke("mgetTwowayAsync")
					.arg(scope);
			break;
		case STREAM:
			throw new UnsupportedOperationException();
		default:
			throw new UnsupportedOperationException();
		}
		return apiInvocation;
	}

	private void wrapOperationParams(JMethod method, JCodeModel codeModel, List<Data<?>> getDatas, JVar dataList) {
		JInvocation dataCreation = null;
		for (Data<?> data : getDatas) {
			method.param(codeModel.directClass(data.getClassName()), data.getName());

			dataCreation = JExpr._new(codeModel.ref(Data.class).narrow(codeModel.directClass(data.getClassName())));
			dataCreation.arg(data.getName()).arg(data.getClassName()).arg(JExpr.lit(data.isPrimitiveType()))
					.arg(JExpr.ref(data.getName())).arg(JExpr.lit(data.getContext().toString())).arg(data.getMediaType().toString());
			JInvocation addData2List = dataList.invoke("add").arg(dataCreation);
			method.body().add(addData2List);
		}
	}

	@Override
	protected void generateRootClass() {
		// TODO Auto-generated method stub
		
		JCodeModel codeModel = new JCodeModel();
		JDefinedClass definitionClass = this.generateDefinedClass(codeModel, "OUTPUTDATATYPE");
		this.addComment(definitionClass, this.classComment);

		definitionClass.annotate(javax.xml.bind.annotation.XmlAccessorType.class).param("value",
				javax.xml.bind.annotation.XmlAccessType.FIELD);

		definitionClass.annotate(javax.xml.bind.annotation.XmlRootElement.class).param("name", "OUTPUTDATATYPE");
		
		
		Collection<Operation> operations = this.componentDescription.getOperations();
		for (Operation operation : operations) {
			
			
			for(Data<?> postAttr : operation.getPostDatas()){
				
				JFieldVar attrField = null;
				if (postAttr.getClassName().indexOf("<") != -1) {
					
					JClass ListClass = null;
					JClass argClass = codeModel.ref(postAttr.getClassName().substring(postAttr.getClassName().indexOf("<") + 1,
							postAttr.getClassName().length() - 1));
					ListClass = codeModel.ref(List.class).narrow(argClass);
					attrField = definitionClass.field(JMod.PRIVATE, ListClass, postAttr.getName());
					attrField.mods().setFinal(false);
					// includePackageReference(attr.getClassName(), codeModel);
				} else {
					attrField = generateAttribute(codeModel, definitionClass, codeModel.directClass(postAttr.getClassName()),
							postAttr.getName(), false);
				}
				JAnnotationUse attrAnnotation = attrField.annotate(codeModel.ref("javax.xml.bind.annotation.XmlElement"));
				attrAnnotation.param("name", postAttr.getName());
				attrAnnotation.param("required", postAttr.isRequired());

				// generate getters
				definitionClass.method(JMod.PUBLIC, attrField.type(), "get" + postAttr.getName()).body()._return(attrField);

				// generate setters
				JMethod setterMethod = definitionClass.method(JMod.PUBLIC, codeModel.VOID, "set" + postAttr.getName());
				setterMethod.param(attrField.type(), postAttr.getName());
				setterMethod.body().assign(JExpr._this().ref(postAttr.getName()), JExpr.ref(postAttr.getName()));

			}
			
		}
		
		this.buildGeneratedClass(codeModel);
	}
	

}


