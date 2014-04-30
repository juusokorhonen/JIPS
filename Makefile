JC = javac
CLASSPATH = ~/Applications//Fiji.app/jars/ij-1.48v.jar:.
CLASSOUTDIR=classes
SRCDIR=src/
INSTALLDIR = ~/Applications/Fiji.app/plugins/JIPS
JFLAGS = -g -sourcepath $(SRCDIR) -classpath $(CLASSPATH) -d $(CLASSOUTDIR)

.PHONY: all clean install

default: all

all: JSM_ScaleBar Megaview_ScaleBar Batch_Converter

JSM_ScaleBar: $(SRCDIR)/JSM_ScaleBar.java
	$(JC) $(JFLAGS) $(SRCDIR)/JSM_ScaleBar.java

Megaview_ScaleBar: $(SRCDIR)/Megaview_ScaleBar.java
	$(JC) $(JFLAGS) $(SRCDIR)/Megaview_ScaleBar.java

Batch_Converter: $(SRCDIR)/Batch_Converter.java
	$(JC) $(JFLAGS) $(SRCDIR)/Batch_Converter.java

install:
	if [ ! -d $(INSTALLDIR) ]; then	mkdir -p $(INSTALLDIR)/JIPS; fi
	cp $(CLASSOUTDIR)/JIPS/*.class $(INSTALLDIR)

clean:
	rm -rf $(CLASSOUTDIR)/JIPS/*.class
