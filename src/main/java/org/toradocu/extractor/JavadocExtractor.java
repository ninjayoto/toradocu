package org.toradocu.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.toradocu.conf.Configuration;
import org.toradocu.doclet.formats.html.ConfigurationImpl;
import org.toradocu.doclet.formats.html.HtmlDocletWriter;
import org.toradocu.doclet.internal.toolkit.taglets.TagletWriter;
import org.toradocu.doclet.internal.toolkit.util.DocFinder;
import org.toradocu.doclet.internal.toolkit.util.DocPath;
import org.toradocu.doclet.internal.toolkit.util.ImplementedMethods;
import org.toradocu.extractor.Method.Builder;
import org.toradocu.util.OutputPrinter;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;

public final class JavadocExtractor {
	
	private final ConfigurationImpl configuration;
	
	public JavadocExtractor(ConfigurationImpl configuration) {
		this.configuration = configuration;
	}

	public List<Method> extract(ClassDoc classDoc) throws IOException {
		TagletWriter tagletWriterInstance = new HtmlDocletWriter(configuration, DocPath.forClass(classDoc)).getTagletWriterInstance(false);   	
		List<Method> methods = new ArrayList<>();
		
		// Loop on constructors and methods (also inherited) of the target class
		for (ExecutableMemberDoc member : getConstructorsAndMethods(classDoc)) {
			Builder methodBuilder = new Method.Builder(member.qualifiedName(), getParameters(member));
			
			List<Tag> tags = new ArrayList<>();
			
			// Collect tags from methods and constructors
			Collections.addAll(tags, member.tags("@throws"));
    		Collections.addAll(tags, member.tags("@exception"));
			// Collect tags automatically inherited (i.e., when there is no comment for method overriding another one)
    		Doc holder = DocFinder.search(new DocFinder.Input(member)).holder;
			Collections.addAll(tags, holder.tags("@throws"));
    		Collections.addAll(tags, holder.tags("@exception"));
    		
			// Collect tags from method definitions in interfaces
			if (holder instanceof MethodDoc) {
				ImplementedMethods implementedMethods = new ImplementedMethods((MethodDoc) holder, configuration);
				for (MethodDoc implementedMethod : implementedMethods.build()) {
					Collections.addAll(tags, implementedMethod.tags("@throws"));
					Collections.addAll(tags, implementedMethod.tags("@exception"));
				}
			}
			
    		for (Tag tag : tags) {
    			if (!(tag instanceof ThrowsTag)) {
    				throw new IllegalStateException("This should not happen. Toradocu only considers @throws tags");
    			}
    			
    			ThrowsTag throwsTag = (ThrowsTag) tag;
    			String comment = tagletWriterInstance.commentTagsToOutput(tag, tag.inlineTags()).toString(); // Inline taglets such as {@inheritDoc}
    			comment = Jsoup.parse(comment).text(); //Remove HTML tags (also generated by taglets inlining)
    			org.toradocu.extractor.ThrowsTag tagToProcess = new org.toradocu.extractor.ThrowsTag(getExceptionName(throwsTag), comment);
    			
    			methodBuilder.tag(tagToProcess);
    		}
    
    		methods.add(methodBuilder.build());
		}
	
		printOutput(methods);
		return methods;
	}
	
	private Parameter[] getParameters(ExecutableMemberDoc member) {
		com.sun.javadoc.Parameter[] params = member.parameters();
		Parameter[] parameters = new Parameter[params.length];
		for (int i = 0; i < parameters.length; i++) {
			Type pType = params[i].type();
			String type = pType.qualifiedTypeName() + pType.dimension();
			parameters[i] = new Parameter(type, params[i].name());
		}
		return parameters;
	}

	private void printOutput(List<Method> collectionToPrint) {
		OutputPrinter.Builder builder = new OutputPrinter.Builder("JavadocExtractor", collectionToPrint);
		OutputPrinter printer = builder.file(Configuration.INSTANCE.getJavadocExtractorOutput()).build();
		printer.print();
	}
	
	/**
	 * This method tries to return the qualified name of the exception in the <code>throwsTag</code>.
	 * If the source code of the exception is not available, type is null. Then
	 * we consider simply the name in the Javadoc comment.
	 * 
	 * @param throwsTag throw tag
	 * @return the exception name
	 */
	private String getExceptionName(ThrowsTag throwsTag) {
		Type exceptionType = throwsTag.exceptionType();
		return exceptionType != null ? exceptionType.qualifiedTypeName() : throwsTag.exceptionName();
	}


	/**
	 * @param classDoc a class
	 * @return the list of constructors and methods of <code>classDoc</code>
	 */
	private List<ExecutableMemberDoc> getConstructorsAndMethods(ClassDoc classDoc) {
		List<ExecutableMemberDoc> membersToAnalyze = new ArrayList<>();
		Set<String> membersAlreadyConsidered = new HashSet<>();
		ClassDoc currentClass = classDoc;
		
		// Add non-default constructors
		for (ConstructorDoc constructor : currentClass.constructors()) {
			// This is a bad workaround to bug in Doc.position() method. It does not return null
			// for default constructors.
			if (!constructor.position().toString().equals(currentClass.position().toString())) {
				membersToAnalyze.add(constructor);
			}
		}
		// Add non-synthetic methods
		do {
			List<ExecutableMemberDoc> currentMembers = new ArrayList<>();
			currentMembers.addAll(Arrays.asList(currentClass.methods()));
			// In this way we consider a method only once even when is overridden
			for (ExecutableMemberDoc member : currentMembers) {
				String memberID = member.name() + member.signature();
				if (!membersAlreadyConsidered.contains(memberID)) {
					if (!member.isSynthetic()) { // Ignore synthetic methods
						membersToAnalyze.add(member);
					}
					membersAlreadyConsidered.add(memberID);
				}
			}
			currentClass = currentClass.superclass();
		} while (currentClass != null && !currentClass.qualifiedName().equals("java.lang.Object"));
		return membersToAnalyze;
	}
}
