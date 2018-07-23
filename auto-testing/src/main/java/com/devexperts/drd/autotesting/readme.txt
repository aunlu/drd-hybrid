Автоматически запускает DRD на тестах из tests на стадии mvn verify.

!!!  избегайте использования пробелов в именах и путях файлов  !!!

Используется config.xml, находящийся в com.devexperts.drd.tests.examples.test_name - свой для каждого теста.

Через config.xml передаются конфигурации для запуска DRD и ожидаемый результат исполнения теста. (см. ниже)
Запускает каждый тест с конфигурациями DRD, указанными в config.xml. Сопоставляет реальные результаты исполнения теста с ожидаемыми.

Результат работы содержится в логах в папке tests\target\auto-logs.

В файле test\target\auto-logs\report.xml в формате JUnit содежится краткая информация о результате исполнения тестов. Полная информация записана в логах.

------------------------------------------------------------------------------------------------------------------------

<Test>
  <DRD>
    <Config name="config_name_1" path="./tests/configs/1"/>
    <Config name="config_name_2" path="./tests/configs/2"/>
  </DRD>
  <Races>
    <Race class="com.devexperts.drd.tests.examples.guaranteed_dr.TestRunner" field="o"/>
    <Race class="com.devexperts.drd_test.test1.User" field="name" config="config_name_1"/>
  </Races>
</Test>


*DRD
Поля name и path обязательны.
path в разделе DRD - это путь к папке с config.xml, drd-properties и hb-config.xml(?).

*Races
Поля class и field обязательны.
config - имя конфига, при котором должна быть найдена описываемая гонка.
При отсутствии поля config, считается, что гонка должна быть найдена для любого конфига.
Чтобы указать наличие гонки с k конфигами, нужно k строк, описывающих гонку - по одной для каждого конфига.

------------------------------------------------------------------------------------------------------------------------

Возможен выборочный запуск тестов.
Для этого в pom.xml в конфигурации запуска тестирования нужно изменить или добавить SystemProperty:
key = drd.auto.testing.include / drd.auto.testing.exclude.
value = имена тестов через запятую без пробелов / * / пустая строка.
(* - обозначение для всех тестов, пустая строка - пустое множество тестов)

При наличии нескольких вариаций property, будет использована последняя.
При отсутствии property для drd.auto.testing.include принимается значение "*", для drd.auto.testing.exclude - "".

При указании теста, как included и excluded одновременно, тест будет обозначен как excluded.
Если тест нигде не указан, он не исполняется.

<configuration>
    ...
    <systemProperties>
        <systemProperty>
            <key>drd.auto.testing.include</key>
            <value>guaranteed_dr,possible_dr</value>
        </systemProperty>

        <systemProperty>
            <key>drd.auto.testing.include</key>
            <value>*</value>
        </systemProperty>

        <systemProperty>
            <key>drd.auto.testing.exclude</key>
            <value></value>
        </systemProperty>
    </systemProperties>
    ...
</configuration>
