//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.1-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.07.04 at 08:48:07 AM BST 
//


package uk.ac.rdg.resc.ncwms.ogc.exceptions;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the uk.ac.rdg.resc.gadswms.ogc.exceptions package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: uk.ac.rdg.resc.gadswms.ogc.exceptions
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceExceptionType }
     * 
     */
    public ServiceExceptionType createServiceExceptionType() {
        return new ServiceExceptionType();
    }

    /**
     * Create an instance of {@link ServiceExceptionReport }
     * 
     */
    public ServiceExceptionReport createServiceExceptionReport() {
        return new ServiceExceptionReport();
    }

}