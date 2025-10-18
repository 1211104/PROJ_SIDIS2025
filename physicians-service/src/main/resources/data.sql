-- Limpeza defensiva (ignorado se vazio)
DELETE FROM physician;

INSERT INTO physician (physician_number, name, specialty, contact_info) VALUES
    ('PH001','Carlos Albergaria','Ossos','carlos.albergaria@hap.xyz'),
    ('PH002','Ana Fonseca','Olhos','ana.fonseca@hap.xyz');
