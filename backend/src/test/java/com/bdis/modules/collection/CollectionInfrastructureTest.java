package com.bdis.modules.collection;

import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class CollectionInfrastructureTest {

    @Test
    void collectionInfrastructureClassesAndMapperXmlShouldExist() throws Exception {
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbCollectionTaskEntity"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbBatchEntity")).isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbBatchImageEntity"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbCollectionTaskMapper"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbBatchMapper")).isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbBatchImageMapper"))
                .isNotNull();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assertThat(classLoader.getResource("mapper/collection/HerbCollectionTaskMapper.xml"))
                .isNotNull();
        assertThat(classLoader.getResource("mapper/collection/HerbBatchMapper.xml")).isNotNull();
        assertThat(classLoader.getResource("mapper/collection/HerbBatchImageMapper.xml"))
                .isNotNull();
    }

    @Test
    void mapperResultPropertiesShouldHaveWritableBeanProperties() throws Exception {
        assertWritableResultProperties("mapper/collection/HerbCollectionTaskMapper.xml");
        assertWritableResultProperties("mapper/collection/HerbBatchMapper.xml");
        assertWritableResultProperties("mapper/collection/HerbBatchImageMapper.xml");
    }

    private void assertWritableResultProperties(String resourcePath) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            assertThat(inputStream).as(resourcePath).isNotNull();
            NodeList resultMaps =
                    factory.newDocumentBuilder()
                            .parse(inputStream)
                            .getElementsByTagName("resultMap");
            for (int index = 0; index < resultMaps.getLength(); index++) {
                Element resultMap = (Element) resultMaps.item(index);
                Class<?> resultType = Class.forName(resultMap.getAttribute("type"));
                Set<String> writableProperties =
                        Arrays.stream(Introspector.getBeanInfo(resultType).getPropertyDescriptors())
                                .filter(descriptor -> descriptor.getWriteMethod() != null)
                                .map(descriptor -> descriptor.getName())
                                .collect(Collectors.toSet());
                NodeList mappings = resultMap.getChildNodes();
                for (int mappingIndex = 0; mappingIndex < mappings.getLength(); mappingIndex++) {
                    Node mapping = mappings.item(mappingIndex);
                    if (mapping instanceof Element element && element.hasAttribute("property")) {
                        String property = element.getAttribute("property");
                        assertThat(writableProperties)
                                .as(
                                        "%s resultMap %s property %s",
                                        resourcePath, resultMap.getAttribute("id"), property)
                                .contains(property);
                    }
                }
            }
        }
    }
}
