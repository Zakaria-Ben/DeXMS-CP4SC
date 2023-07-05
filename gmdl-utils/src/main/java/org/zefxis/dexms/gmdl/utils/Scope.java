package org.zefxis.dexms.gmdl.utils;

import org.zefxis.dexms.gmdl.utils.enums.Verb;

// TODO: Scope should be specialized for each protocol's required information
public class Scope {
  
  /* REST */
  private String name;
  private String uri; 
  private Verb verb;
  private String expression;
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
  
  public Verb getVerb() {
    return verb;
  }

  public void setVerb(Verb verb){
    this.verb = verb;
    
  }
  
  public String getExpression() {
    return expression;
  }
  
  public void setExpression(String expression) {
    this.expression = expression;
  }
}