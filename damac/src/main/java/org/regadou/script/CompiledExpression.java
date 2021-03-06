package org.regadou.script;

import java.util.*;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.damai.Reference;
import org.regadou.reference.ReferenceHolder;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.reference.MapEntryWrapper;

public class CompiledExpression extends CompiledScript implements Expression {

   private Configuration configuration;
   private ScriptEngine engine;
   private String text;
   private List<Reference> tokens = new ArrayList<>();
   private Action action;

   public CompiledExpression(ScriptEngine engine, Collection<Reference> tokens, Configuration configuration) {
      this.engine = engine;
      this.configuration = configuration;
      if (tokens != null) {
         for (Reference token : tokens)
            addToken(token);
      }
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object eval(ScriptContext context) throws ScriptException {
      return getValue(context);
   }

   @Override
   public ScriptEngine getEngine() {
      return engine;
   }

   @Override
   public String getName() {
      if (text == null) {
         text = (action == null) ? "" : action.getName();
         for (Object token : tokens) {
            if (!text.isEmpty())
               text += " ";
            text += String.valueOf(token);
         }
         text = "("+text+")";
      }
      return text;
   }

   @Override
   public Reference getValue() {
      return getValue(null);
   }

   @Override
   public Class<Reference> getType() {
      return Reference.class;
   }

   @Override
   public void setValue(Reference value) {
      Reference result = getValue();
      if (result != null)
         result.setValue(value);
   }

   @Override
   public Action getAction() {
      return action;
   }

   @Override
   public Reference[] getTokens() {
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public void addToken(Reference token) {
      text = null;
      if (action == null && (action = isAction(token)) != null) {
         if (tokens.size() > 1) {
             //TODO: try to make an entity out of it
            Reference subject = new ReferenceHolder(null, tokens);
            tokens = new ArrayList();
            tokens.add(subject);
         }
      }
      else
         tokens.add(token);
   }

   @Override
   public Reference getValue(ScriptContext context) {
      if (context == null)
         context = configuration.getContextFactory().getScriptContext();
      if (action != null) {
         if (tokens.isEmpty())
            return new ReferenceHolder(action.getName(), action);
         Object value = action.execute(tokens.toArray());
         if (value instanceof Reference)
            return (Reference)value;
         else if (value instanceof Map.Entry)
            return new MapEntryWrapper((Map.Entry)value);
         else
            return new ReferenceHolder(null, value);
      }
      else {
         switch (tokens.size()) {
            case 0:
               return null;
            case 1:
               return tokens.get(0);
            default:
               if (tokens.get(0) instanceof Expression) {
                  Reference result = null;
                  for (Reference token : tokens)
                     result = ((Expression)token).getValue();
                  return result;
               }
               //TODO: check if we have properties enumeration for an entity
               return new ReferenceHolder(null, tokens);
         }
      }
   }

   private Action isAction(Object token) {
      if (token instanceof Action) {
         return (Action)token;
      }
      else if (token instanceof Reference) {
         return isAction(((Reference)token).getValue());
      }
      else {
         return null;
      }
   }
}
